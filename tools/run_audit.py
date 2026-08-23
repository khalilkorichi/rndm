#!/usr/bin/env python3
"""
RNDM Inspector - One-Click Launcher
Starts the local audit engine server and opens the interactive dashboard in the default web browser.
"""

import sys
import os
import time
import webbrowser
import threading
from pathlib import Path

# Add tools directory to path
TOOLS_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = TOOLS_DIR.parent
sys.path.insert(0, str(TOOLS_DIR))

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

from audit_engine.server import start_server

PORT = 4321
URL = f"http://localhost:{PORT}"

def open_browser_delayed():
    time.sleep(1.2)
    print(f"Opening dashboard in browser: {URL}")
    webbrowser.open(URL)

def main():
    print("============================================================")
    print("RNDM Inspector - Code & Database Audit Engine")
    print(f"Project Root: {PROJECT_ROOT}")
    print(f"Dashboard URL: {URL}")
    print("============================================================")
    
    # Launch browser in separate thread
    threading.Thread(target=open_browser_delayed, daemon=True).start()
    
    # Start server
    start_server(port=PORT)

if __name__ == "__main__":
    main()
