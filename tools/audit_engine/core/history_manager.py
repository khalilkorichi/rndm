"""
RNDM Inspector - History & Diff Manager
Maintains audit scan history, diff tracking (resolved vs new issues), and health score calculation.
"""

import json
import time
from pathlib import Path
from typing import Dict, List, Any, Optional

class HistoryManager:
    def __init__(self, storage_path: str):
        self.storage_path = Path(storage_path)

    def load_history(self) -> List[Dict[str, Any]]:
        if not self.storage_path.exists():
            return []
        try:
            data = json.loads(self.storage_path.read_text(encoding="utf-8"))
            return data if isinstance(data, list) else []
        except Exception:
            return []

    def get_latest_scan(self) -> Optional[Dict[str, Any]]:
        history = self.load_history()
        return history[-1] if history else None

    def compute_health_score(self, issues: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Computes a 0-100 health score and letter grade."""
        weights = {
            "CRITICAL": 15,
            "HIGH": 8,
            "MEDIUM": 3,
            "LOW": 1,
            "INFO": 0
        }
        penalty = sum(weights.get(i.get("severity", "LOW"), 1) for i in issues)
        score = max(0, min(100, 100 - penalty))

        if score >= 95:
            grade = "A+"
            color = "#10B981"  # Emerald
            status_ar = "ممتاز جداً"
        elif score >= 85:
            grade = "A"
            color = "#10B981"
            status_ar = "ممتاز"
        elif score >= 75:
            grade = "B"
            color = "#3B82F6"  # Blue
            status_ar = "جيد جداً"
        elif score >= 60:
            grade = "C"
            color = "#F59E0B"  # Amber
            status_ar = "متوسط - بحاجة لتحسين"
        elif score >= 45:
            grade = "D"
            color = "#F97316"  # Orange
            status_ar = "ضعيف - يحتاج معالجة"
        else:
            grade = "F"
            color = "#EF4444"  # Red
            status_ar = "حرج - يتطلب تدخلاً فورياً"

        return {
            "score": score,
            "grade": grade,
            "color": color,
            "status_ar": status_ar,
            "total_penalty": penalty
        }

    def record_scan(self, scan_result: Dict[str, Any]) -> Dict[str, Any]:
        """Saves current scan and returns diff comparison against previous scan."""
        history = self.load_history()
        prev_scan = history[-1] if history else None

        current_issues = scan_result.get("issues", [])
        current_ids = {i["id"]: i for i in current_issues if "id" in i}

        resolved_issues = []
        new_issues = []
        persistent_issues = []

        if prev_scan:
            prev_issues = prev_scan.get("issues", [])
            prev_ids = {i["id"]: i for i in prev_issues if "id" in i}

            for p_id, p_issue in prev_ids.items():
                if p_id not in current_ids:
                    resolved_issues.append(p_issue)

            for c_id, c_issue in current_ids.items():
                if c_id not in prev_ids:
                    new_issues.append(c_issue)
                else:
                    persistent_issues.append(c_issue)
        else:
            new_issues = list(current_issues)

        health = self.compute_health_score(current_issues)

        record = {
            "timestamp": int(time.time()),
            "date_str": time.strftime("%Y-%m-%d %H:%M:%S"),
            "health": health,
            "summary": {
                "total": len(current_issues),
                "critical": sum(1 for i in current_issues if i.get("severity") == "CRITICAL"),
                "high": sum(1 for i in current_issues if i.get("severity") == "HIGH"),
                "medium": sum(1 for i in current_issues if i.get("severity") == "MEDIUM"),
                "low": sum(1 for i in current_issues if i.get("severity") == "LOW"),
                "info": sum(1 for i in current_issues if i.get("severity") == "INFO"),
                "resolved_since_last": len(resolved_issues),
                "new_since_last": len(new_issues)
            },
            "diff": {
                "resolved_count": len(resolved_issues),
                "new_count": len(new_issues),
                "persistent_count": len(persistent_issues),
                "resolved_issues": resolved_issues,
                "new_issues": new_issues
            },
            "database_summary": scan_result.get("database", {}).get("summary", {}),
            "issues": current_issues
        }

        # Keep last 50 scans
        history.append(record)
        if len(history) > 50:
            history = history[-50:]

        try:
            self.storage_path.parent.mkdir(parents=True, exist_ok=True)
            self.storage_path.write_text(json.dumps(history, indent=2, ensure_ascii=False), encoding="utf-8")
        except Exception as e:
            print(f"Error saving history: {e}")

        return record
