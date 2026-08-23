"""
RNDM Inspector - Fast Local HTTP Server & REST API
Built using Python standard library (zero third-party dependencies required).
"""

import os
import sys
import json
from pathlib import Path
from http.server import HTTPServer, SimpleHTTPRequestHandler
import urllib.parse
from typing import Dict, Any

# Ensure audit_engine parent directory is in sys.path
BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BASE_DIR.parent.parent
if str(BASE_DIR.parent) not in sys.path:
    sys.path.insert(0, str(BASE_DIR.parent))

try:
    from audit_engine.core.analyzer import MasterAnalyzer
    from audit_engine.core.history_manager import HistoryManager
    from audit_engine.core.exporter import ReportExporter
except ImportError:
    from core.analyzer import MasterAnalyzer
    from core.history_manager import HistoryManager
    from core.exporter import ReportExporter

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
WEB_DIR = Path(__file__).resolve().parent / "web"

class AuditRequestHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_DIR), **kwargs)

    def do_GET(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path
        query_params = urllib.parse.parse_qs(parsed_url.query)

        if path == "/api/scan" or path == "/api/run-scan":
            self._handle_run_scan()
        elif path == "/api/latest":
            self._handle_get_latest()
        elif path == "/api/history":
            self._handle_get_history()
        elif path == "/api/file-preview":
            self._handle_file_preview(query_params)
        elif path == "/" or path == "/index.html":
            self._serve_file(WEB_DIR / "index.html", "text/html; charset=utf-8")
        elif path == "/style.css":
            self._serve_file(WEB_DIR / "style.css", "text/css; charset=utf-8")
        elif path == "/app.js":
            self._serve_file(WEB_DIR / "app.js", "application/javascript; charset=utf-8")
        else:
            # Fallback to static file server
            target_path = WEB_DIR / path.lstrip("/")
            if target_path.exists() and target_path.is_file():
                super().do_GET()
            else:
                self._serve_file(WEB_DIR / "index.html", "text/html; charset=utf-8")

    def do_POST(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path

        if path == "/api/scan" or path == "/api/run-scan":
            self._handle_run_scan()
        elif path == "/api/export/markdown":
            self._handle_export_markdown()
        elif path == "/api/export/single-markdown":
            self._handle_export_single_markdown()
        else:
            self._send_json({"error": "Not Found"}, status=404)

    def _handle_run_scan(self):
        try:
            analyzer = MasterAnalyzer(str(PROJECT_ROOT))
            result = analyzer.run_full_scan()
            self._send_json(result)
        except Exception as e:
            self._send_json({"error": str(e)}, status=500)

    def _handle_get_latest(self):
        history_path = Path(__file__).resolve().parent / "audit_history.json"
        mgr = HistoryManager(str(history_path))
        latest = mgr.get_latest_scan()
        if latest:
            self._send_json(latest)
        else:
            # Run initial scan if empty
            self._handle_run_scan()

    def _handle_get_history(self):
        history_path = Path(__file__).resolve().parent / "audit_history.json"
        mgr = HistoryManager(str(history_path))
        history = mgr.load_history()
        self._send_json({"history": history})

    def _handle_file_preview(self, query_params):
        rel_path = query_params.get("path", [""])[0]
        line_str = query_params.get("line", ["1"])[0]
        try:
            line_no = int(line_str)
        except ValueError:
            line_no = 1

        target_file = PROJECT_ROOT / rel_path
        if not target_file.exists() or not target_file.is_file():
            self._send_json({"error": "File not found", "path": rel_path}, status=404)
            return

        try:
            content = target_file.read_text(encoding="utf-8", errors="ignore")
            lines = content.split('\n')
            start = max(0, line_no - 10)
            end = min(len(lines), line_no + 10)

            snippet_lines = []
            for i in range(start, end):
                snippet_lines.append({
                    "line_number": i + 1,
                    "content": lines[i],
                    "is_target": (i + 1 == line_no)
                })

            self._send_json({
                "file": rel_path,
                "target_line": line_no,
                "total_lines": len(lines),
                "snippet": snippet_lines
            })
        except Exception as e:
            self._send_json({"error": str(e)}, status=500)

    def _handle_export_markdown(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else "{}"
        try:
            data = json.loads(body)
        except Exception:
            data = {}

        if not data:
            history_path = Path(__file__).resolve().parent / "audit_history.json"
            mgr = HistoryManager(str(history_path))
            data = mgr.get_latest_scan() or {}

        md = ReportExporter.generate_full_markdown_report(data)
        self._send_json({"markdown": md})

    def _handle_export_single_markdown(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else "{}"
        try:
            issue = json.loads(body)
            md = ReportExporter.format_single_issue_markdown(issue)
            self._send_json({"markdown": md})
        except Exception as e:
            self._send_json({"error": str(e)}, status=400)

    def _serve_file(self, file_path: Path, content_type: str):
        if not file_path.exists():
            self._send_json({"error": "File Not Found"}, status=404)
            return

        content = file_path.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(content)))
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.end_headers()
        self.wfile.write(content)

    def _send_json(self, data: Any, status: int = 200):
        body = json.dumps(data, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()
        self.wfile.write(body)

def start_server(port: int = 4321):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    server_address = ('127.0.0.1', port)
    httpd = HTTPServer(server_address, AuditRequestHandler)
    print("============================================================")
    print("RNDM Inspector - Code & Database Audit Suite is Running!")
    print(f"Local Dashboard: http://localhost:{port}")
    print("============================================================")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...")
        httpd.server_close()

if __name__ == "__main__":
    start_server()
