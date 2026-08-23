"""
RNDM Inspector - Code Quality, Clean Architecture & Memory Leak Analyzer
Static analysis for Android architectural layering, memory leaks, and clean code principles.
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Any

class CodeQualityAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.issues: List[Dict[str, Any]] = []

    def analyze(self) -> Dict[str, Any]:
        """Runs architectural and clean code analysis."""
        self.issues = []
        app_dir = self.project_root / "app" / "src" / "main" / "java"
        if not app_dir.exists():
            return {"issues": self.issues}

        for kt_file in app_dir.rglob("*.kt"):
            try:
                content = kt_file.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue

            rel_path = str(kt_file.relative_to(self.project_root)).replace("\\", "/")
            self._scan_file_quality(kt_file, rel_path, content)

        return {
            "summary": {
                "quality_issues_count": len(self.issues)
            },
            "issues": self.issues
        }

    def _scan_file_quality(self, file_path: Path, rel_path: str, content: str):
        lines = content.split('\n')

        # 1. Check Context Leak in ViewModel
        if "ViewModel" in rel_path:
            for idx, line in enumerate(lines, 1):
                if re.search(r'val\s+[a-zA-Z0-9_]+\s*:\s*(?:Activity|Context)\b', line) and "Application" not in line:
                    self.issues.append({
                        "id": f"ARCH-CONTEXT-LEAK-VIEWMODEL-{Path(rel_path).stem}-{idx}",
                        "title": "Potential Context / Activity Leak in ViewModel",
                        "title_ar": "احتمالية تسريب الذاكرة (Context/Activity Leak) داخل ViewModel",
                        "category": "Memory & Leaks",
                        "category_ar": "الذاكرة وتسريبات الموارد",
                        "severity": "CRITICAL",
                        "file": rel_path,
                        "line": idx,
                        "code_snippet": line.strip(),
                        "description": "Holding a reference to an Activity or UI Context inside a ViewModel prevents the Activity from being garbage-collected on screen rotation or recreation, leading to permanent memory leaks.",
                        "description_ar": "الاحتفاظ بمرجع لـ Activity أو Context داخل الـ ViewModel يمنع نظام أندرويد من تحرير ذاكرة الشاشة عند تدوير الشاشة أو إغلاقها، مما يسبب تسريباً دائماً للذاكرة (Memory Leak).",
                        "fix_suggestion": "Use AndroidViewModel with `Application` context, or pass context into specific method calls instead of storing it.",
                        "fix_code": "class MyViewModel(application: Application) : AndroidViewModel(application) { ... }"
                    })

        # 2. Check Direct DAO Access in Presentation Layer
        if "/presentation/" in rel_path:
            for idx, line in enumerate(lines, 1):
                if re.search(r'\b[A-Za-z0-9_]+Dao\b', line) and "import" not in line:
                    self.issues.append({
                        "id": f"ARCH-DIRECT-DAO-IN-UI-{Path(rel_path).stem}-{idx}",
                        "title": "Direct DAO access in Presentation Layer (Clean Architecture Violation)",
                        "title_ar": "استدعاء مباشر لقواعد البيانات (DAO) داخل طبقة العرض Presentation",
                        "category": "Architecture & Clean Code",
                        "category_ar": "جودة الكود والبنية",
                        "severity": "HIGH",
                        "file": rel_path,
                        "line": idx,
                        "code_snippet": line.strip(),
                        "description": "Presentation components (Screens, ViewModels) should not directly interact with Room DAOs. Data access must be encapsulated through Repositories and UseCases to maintain testability and separation of concerns.",
                        "description_ar": "مكونات واجهة العرض لا يجب أن تتواصل مباشرة مع كائنات DAO. يجب تغليف عمليات البيانات عبر طبقة Repository و UseCases للحفاظ على قابلية الفحص واستقلالية الطبقات.",
                        "fix_suggestion": "Inject and use a Repository interface instead of a direct DAO.",
                        "fix_code": "class MyViewModel(private val repository: TournamentRepository) : ViewModel()"
                    })

        # 3. Check for TODO / FIXME leftovers
        for idx, line in enumerate(lines, 1):
            if re.search(r'//\s*(?:TODO|FIXME)\b', line, re.IGNORECASE):
                self.issues.append({
                    "id": f"QUALITY-TODO-FOUND-{Path(rel_path).stem}-{idx}",
                    "title": f"Unresolved TODO / FIXME in `{Path(rel_path).name}`",
                    "title_ar": f"ملاحظة برمجية غير مكتملة TODO / FIXME في `{Path(rel_path).name}`",
                    "category": "Architecture & Clean Code",
                    "category_ar": "جودة الكود والبنية",
                    "severity": "INFO",
                    "file": rel_path,
                    "line": idx,
                    "code_snippet": line.strip(),
                    "description": "Unresolved TODO or FIXME comment left in production code indicates unfinished feature or potential technical debt.",
                    "description_ar": "وجود وسوم TODO أو FIXME في الكود المصدري يشير إلى ميزات غير مكتملة أو ديون برمجية بحاجة للمراجعة والإغلاق.",
                    "fix_suggestion": "Implement the missing logic or remove the stale comment.",
                    "fix_code": line.strip()
                })
