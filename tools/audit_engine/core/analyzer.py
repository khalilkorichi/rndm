"""
RNDM Inspector - Master Static Analysis Engine
Coordinates all specialized analyzers to produce a comprehensive code & database audit.
"""

import time
from pathlib import Path
from typing import Dict, List, Any

from .db_analyzer import DatabaseAnalyzer
from .compose_analyzer import ComposeAnalyzer
from .concurrency_analyzer import ConcurrencyAnalyzer
from .code_quality_analyzer import CodeQualityAnalyzer
from .history_manager import HistoryManager

class MasterAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.history_path = self.project_root / "tools" / "audit_engine" / "audit_history.json"
        self.history_mgr = HistoryManager(str(self.history_path))

    def run_full_scan(self) -> Dict[str, Any]:
        """Executes full multi-pass analysis on the codebase."""
        start_time = time.time()

        # Instantiate analyzers
        db_analyzer = DatabaseAnalyzer(str(self.project_root))
        compose_analyzer = ComposeAnalyzer(str(self.project_root))
        concurrency_analyzer = ConcurrencyAnalyzer(str(self.project_root))
        quality_analyzer = CodeQualityAnalyzer(str(self.project_root))

        # Run scans
        db_results = db_analyzer.analyze()
        compose_results = compose_analyzer.analyze()
        concurrency_results = concurrency_analyzer.analyze()
        quality_results = quality_analyzer.analyze()

        # Combine all issues
        all_issues = []
        all_issues.extend(db_results.get("issues", []))
        all_issues.extend(compose_results.get("issues", []))
        all_issues.extend(concurrency_results.get("issues", []))
        all_issues.extend(quality_results.get("issues", []))

        # Sort issues by severity priority
        severity_order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "INFO": 4}
        all_issues.sort(key=lambda x: severity_order.get(x.get("severity", "LOW"), 99))

        duration_ms = int((time.time() - start_time) * 1000)

        # Count total Kotlin and XML files
        total_files = len(list((self.project_root / "app" / "src" / "main").rglob("*.kt")))
        total_xml = len(list((self.project_root / "app" / "src" / "main").rglob("*.xml")))

        scan_payload = {
            "duration_ms": duration_ms,
            "total_files_scanned": total_files + total_xml,
            "database": db_results,
            "compose": compose_results,
            "concurrency": concurrency_results,
            "quality": quality_results,
            "issues": all_issues
        }

        # Record to history and get diff
        recorded_record = self.history_mgr.record_scan(scan_payload)
        
        # Combine final response
        response = {
            **recorded_record,
            "duration_ms": duration_ms,
            "total_files_scanned": total_files + total_xml,
            "database_details": {
                "entities": db_results.get("entities", {}),
                "daos": db_results.get("daos", {}),
                "migrations": db_results.get("migrations", []),
                "database_meta": db_results.get("database_meta", {})
            }
        }

        return response
