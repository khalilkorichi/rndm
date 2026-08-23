"""
RNDM Inspector - Concurrency & Coroutines Analyzer
Static analysis for Kotlin Coroutines, Thread Safety, Dispatchers, and Lifecycle Flow Collections.
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Any

class ConcurrencyAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.issues: List[Dict[str, Any]] = []

    def analyze(self) -> Dict[str, Any]:
        """Runs concurrency and coroutine safety analysis."""
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
            self._scan_concurrency_file(kt_file, rel_path, content)

        return {
            "summary": {
                "concurrency_issues_count": len(self.issues)
            },
            "issues": self.issues
        }

    def _scan_concurrency_file(self, file_path: Path, rel_path: str, content: str):
        lines = content.split('\n')

        for idx, line in enumerate(lines, 1):
            # 1. Check Thread.sleep
            if "Thread.sleep(" in line:
                self.issues.append({
                    "id": f"CONCURRENCY-THREAD-SLEEP-{Path(rel_path).stem}-{idx}",
                    "title": "Blocking `Thread.sleep()` used instead of Coroutine `delay()`",
                    "title_ar": "استخدام `Thread.sleep()` المعطل للخيط بدلاً من `delay()` غير الحاجز",
                    "category": "Concurrency & Coroutines",
                    "category_ar": "التزامن وخيوط المعالجة",
                    "severity": "CRITICAL",
                    "file": rel_path,
                    "line": idx,
                    "code_snippet": line.strip(),
                    "description": "Calling `Thread.sleep()` blocks the underlying operating system thread completely. If called on Dispatchers.Main, it freezes the entire application UI triggering an ANR (Application Not Responding).",
                    "description_ar": "استدعاء `Thread.sleep()` يقوم بحجز وتجميد خيط المعالجة بالكامل. عند تنفيذه على خيط الواجهة الرئيسي، يؤدي لتوقف الشاشة وانهيار التطبيق بخطأ ANR.",
                    "fix_suggestion": "Replace `Thread.sleep(millis)` with non-blocking `delay(millis)` in coroutines.",
                    "fix_code": line.replace("Thread.sleep", "delay").strip()
                })

            # 2. Check GlobalScope usage
            if "GlobalScope." in line:
                self.issues.append({
                    "id": f"CONCURRENCY-GLOBALSCOPE-{Path(rel_path).stem}-{idx}",
                    "title": "Delicate Coroutine API `GlobalScope` usage",
                    "title_ar": "استخدام النطاق العام `GlobalScope` المعرض لتسريبات الذاكرة",
                    "category": "Concurrency & Coroutines",
                    "category_ar": "التزامن وخيوط المعالجة",
                    "severity": "HIGH",
                    "file": rel_path,
                    "line": idx,
                    "code_snippet": line.strip(),
                    "description": "`GlobalScope` creates top-level coroutines that are not bound to any Android lifecycle, preventing garbage collection and leaking memory if the user navigates away.",
                    "description_ar": "`GlobalScope` ينشئ مهام غير مرتبطة بدورة حياة الشاشة أو الـ ViewModel، مما يمنع تنظيف الذاكرة ويسبب تسريباً واستهلاكاً مستمراً للبطارية بالخلفية.",
                    "fix_suggestion": "Use `viewModelScope` in ViewModels or structured scopes.",
                    "fix_code": "viewModelScope.launch {\n    // Async task\n}"
                })

            # 3. Check collectAsState() vs collectAsStateWithLifecycle()
            if re.search(r'\bcollectAsState\s*\(', line) and "collectAsStateWithLifecycle" not in line:
                self.issues.append({
                    "id": f"CONCURRENCY-COLLECT-AS-STATE-LIFECYCLE-{Path(rel_path).stem}-{idx}",
                    "title": "Flow collected with `collectAsState()` instead of `collectAsStateWithLifecycle()`",
                    "title_ar": "جمع الـ Flow بواسطة `collectAsState()` دون مراعاة دورة الحياة (Lifecycle)",
                    "category": "Compose Performance",
                    "category_ar": "أداء واجهات Compose",
                    "severity": "MEDIUM",
                    "file": rel_path,
                    "line": idx,
                    "code_snippet": line.strip(),
                    "description": "`collectAsState()` continues collecting Flow emissions even when the application is backgrounded or the screen is covered, keeping database/location/network streams active and wasting battery.",
                    "description_ar": "`collectAsState()` يستمر في استقبال بيانات الـ Flow حتى عند وضع التطبيق في الخلفية، مما يبقي استعلامات قواعد البيانات والشبكة نشطة ويستنزف البطارية.",
                    "fix_suggestion": "Use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose`.",
                    "fix_code": line.replace("collectAsState()", "collectAsStateWithLifecycle()").strip()
                })

            # 4. Check exposed MutableStateFlow in ViewModel
            if "ViewModel" in rel_path and re.search(r'val\s+[a-zA-Z0-9_]+\s*:\s*MutableStateFlow', line) and "private" not in line:
                self.issues.append({
                    "id": f"ARCH-MUTABLE-STATEFLOW-EXPOSED-{Path(rel_path).stem}-{idx}",
                    "title": "MutableStateFlow exposed publicly in ViewModel",
                    "title_ar": "كشف MutableStateFlow للعامة بدون حماية (تخريب نمط أحادي الاتجاه UDF)",
                    "category": "Architecture & Clean Code",
                    "category_ar": "جودة الكود والبنية",
                    "severity": "MEDIUM",
                    "file": rel_path,
                    "line": idx,
                    "code_snippet": line.strip(),
                    "description": "Exposing a `MutableStateFlow` allows UI components or external classes to mutate the ViewModel state directly from the outside, violating the Unidirectional Data Flow (UDF) pattern and making state tracking unpredictable.",
                    "description_ar": "كشف `MutableStateFlow` يتيح لواجهات المستخدم أو الطبقات الخارجية تعديل الحالة مباشرة من الخارج، مما يكسر نمط تدفق البيانات أحادي الاتجاه (UDF) ويصعب تتبع الأخطاء.",
                    "fix_suggestion": "Make the MutableStateFlow private and expose an immutable `StateFlow` using `.asStateFlow()`.",
                    "fix_code": "private val _state = MutableStateFlow(InitialState)\nval state: StateFlow<StateType> = _state.asStateFlow()"
                })

            # 5. Check swallowed CancellationException in coroutine catch blocks
            if re.search(r'catch\s*\(\s*[a-zA-Z0-9_]+\s*:\s*Exception\s*\)', line):
                # Look ahead for rethrow or empty block
                chunk = "\n".join(lines[idx-1:min(len(lines), idx + 6)])
                if "if (" not in chunk and "throw" not in chunk and "CancellationException" not in chunk:
                    # Check if inside a suspend fun or launch block
                    is_coroutine = False
                    for p in range(max(0, idx - 15), idx):
                        if "suspend fun" in lines[p] or "launch" in lines[p] or "async" in lines[p]:
                            is_coroutine = True
                            break
                    if is_coroutine:
                        self.issues.append({
                            "id": f"CONCURRENCY-SWALLOWED-CANCELLATION-{Path(rel_path).stem}-{idx}",
                            "title": "Generic `catch (e: Exception)` may swallow `CancellationException`",
                            "title_ar": "التقاط عام للاستثناءات `catch (e: Exception)` قد يبتلع `CancellationException`",
                            "category": "Concurrency & Coroutines",
                            "category_ar": "التزامن وخيوط المعالجة",
                            "severity": "LOW",
                            "file": rel_path,
                            "line": idx,
                            "code_snippet": line.strip(),
                            "description": "In Kotlin Coroutines, cancelling a job throws `CancellationException`. Catching generic `Exception` without re-throwing `CancellationException` prevents coroutines from stopping cleanly when the ViewModel or Screen is closed.",
                            "description_ar": "في Kotlin Coroutines، إلغاء المهمة يتم عبر رمي `CancellationException`. التقاط كل الاستثناءات دون إعادة رمي الإلغاء يعطل التوقف السليم للمهام عند إغلاق الشاشة.",
                            "fix_suggestion": "Ensure CancellationException is re-thrown or catch specific exceptions.",
                            "fix_code": "catch (e: Exception) {\n    if (e is CancellationException) throw e\n    // Handle expected errors\n}"
                        })
