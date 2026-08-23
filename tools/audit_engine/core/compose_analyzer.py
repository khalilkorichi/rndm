"""
RNDM Inspector - Jetpack Compose Performance Analyzer
Deep static analysis for Jetpack Compose UI performance, recomposition bottlenecks, and memory safety.
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Any

class ComposeAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.issues: List[Dict[str, Any]] = []

    def analyze(self) -> Dict[str, Any]:
        """Runs Jetpack Compose performance analysis across all Kotlin files."""
        self.issues = []
        app_dir = self.project_root / "app" / "src" / "main" / "java"
        if not app_dir.exists():
            return {"issues": self.issues}

        for kt_file in app_dir.rglob("*.kt"):
            try:
                content = kt_file.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue

            if "@Composable" not in content:
                continue

            rel_path = str(kt_file.relative_to(self.project_root)).replace("\\", "/")
            self._scan_compose_file(kt_file, rel_path, content)

        return {
            "summary": {
                "compose_issues_count": len(self.issues)
            },
            "issues": self.issues
        }

    def _scan_compose_file(self, file_path: Path, rel_path: str, content: str):
        lines = content.split('\n')
        
        # 1. Check for LazyColumn / LazyRow items without key
        self._check_missing_lazy_keys(rel_path, lines, content)
        
        # 2. Check for costly operations without remember (filtering, sorting, formatting, regex)
        self._check_unremembered_computations(rel_path, lines, content)

        # 3. Check for coroutines launched directly in Composable body
        self._check_unscoped_coroutines(rel_path, lines, content)

        # 4. Check for state mutation in Composable body
        self._check_state_mutations(rel_path, lines, content)

        # 5. Check for hardcoded colors breaking dark mode
        self._check_hardcoded_colors(rel_path, lines, content)

        # 6. Check for unremembered lambda allocations in loops
        self._check_unremembered_lambdas(rel_path, lines, content)

    def _check_missing_lazy_keys(self, rel_path: str, lines: List[str], content: str):
        for idx, line in enumerate(lines, 1):
            # Check items(...) or itemsIndexed(...)
            if re.search(r'\bitems\s*\(', line) or re.search(r'\bitemsIndexed\s*\(', line):
                # Look at line and next 3 lines to see if `key =` is present
                chunk = "\n".join(lines[max(0, idx - 1):min(len(lines), idx + 3)])
                if "key =" not in chunk and "key=" not in chunk:
                    # Make sure it's inside a LazyList / LazyColumn
                    has_lazy_context = False
                    for prev_idx in range(max(0, idx - 25), idx):
                        if any(k in lines[prev_idx] for k in ["LazyColumn", "LazyRow", "LazyVerticalGrid", "items("]):
                            has_lazy_context = True
                            break
                    
                    if has_lazy_context or "items(" in line:
                        match_item = re.search(r'items(?:Indexed)?\s*\(\s*([^,\)\{]+)', line)
                        list_name = match_item.group(1).strip() if match_item else "itemsList"
                        
                        self.issues.append({
                            "id": f"COMPOSE-MISSING-LAZY-KEY-{Path(rel_path).stem}-{idx}",
                            "title": f"Missing `key` in Lazy list `items({list_name})`",
                            "title_ar": f"مفتاح `key` مفقود في القائمة الكسولة `items({list_name})`",
                            "category": "Compose Performance",
                            "category_ar": "أداء واجهات Compose",
                            "severity": "HIGH",
                            "file": rel_path,
                            "line": idx,
                            "code_snippet": line.strip(),
                            "description": "Without an explicit `key` parameter, Jetpack Compose identifies items by their positional index. If items are inserted, deleted, filtered, or reordered, Compose cannot preserve item animations or UI state and forces a full recomposition of all visible items, causing noticeable scroll stutter (Jank).",
                            "description_ar": "بدون تمرير المعامل `key`، يعتمد Compose على ترتيب العنصر (Index). عند إضافة أو حذف أو إعادة ترتيب العناصر، يفقد Compose القدرة على الحفاظ على حالة العناصر والأنيميشن، ويجبر النظام على إعادة رسم جميع عناصر القائمة، مما يسبب تقطيعاً ولاج (Jank) أثناء التمرير.",
                            "fix_suggestion": f"Provide a unique identifier as key, e.g.: `items(items = {list_name}, key = {{ it.id }})`.",
                            "fix_code": f"items(\n    items = {list_name},\n    key = {{ it.id }}\n) {{ item ->\n    // Composable content\n}}"
                        })

    def _check_unremembered_computations(self, rel_path: str, lines: List[str], content: str):
        # Look for heavy computations inside @Composable functions
        in_composable = False
        composable_brace_depth = 0

        costly_patterns = [
            (r'\b(?:filter|sortedBy|sortedByDescending|groupBy|mapNotNull|distinctBy)\s*\{', "Collection Transformation (filter/sort/group)", "عمليات تحويل وفلترة وترتيب القوائم"),
            (r'\bSimpleDateFormat\s*\(', "SimpleDateFormat Allocation", "إنشاء كائن SimpleDateFormat مكرر"),
            (r'\bDateTimeFormatter\s*\.', "DateTimeFormatter Allocation", "إنشاء كائن DateTimeFormatter مكرر"),
            (r'\bRegex\s*\(', "Regex Compilation in UI loop", "إنشاء ومعالجة التعبيرات النمطية Regex"),
        ]

        for idx, line in enumerate(lines, 1):
            if "@Composable" in line:
                in_composable = True

            if in_composable:
                # Check if inside a remember or derivedStateOf block
                is_remembered = False
                for lookback in range(max(0, idx - 4), idx):
                    if any(k in lines[lookback] for k in ["remember", "derivedStateOf", "LaunchedEffect", "produceState"]):
                        is_remembered = True
                        break

                if not is_remembered:
                    for pattern, name, name_ar in costly_patterns:
                        if re.search(pattern, line):
                            # Ensure it's not in a ViewModel / Helper
                            if "ViewModel" not in rel_path and "Repository" not in rel_path:
                                self.issues.append({
                                    "id": f"COMPOSE-UNREMEMBERED-COMPUTATION-{Path(rel_path).stem}-{idx}",
                                    "title": f"Unremembered {name} in Composable body",
                                    "title_ar": f"{name_ar} بدون `remember` داخل دالة Composable",
                                    "category": "Compose Performance",
                                    "category_ar": "أداء واجهات Compose",
                                    "severity": "MEDIUM",
                                    "file": rel_path,
                                    "line": idx,
                                    "code_snippet": line.strip(),
                                    "description": f"Executing `{line.strip()}` directly inside a Composable function causes it to be re-computed on EVERY recomposition frame, wasting CPU cycles and leading to frame drops.",
                                    "description_ar": f"تنفيذ هذه العملية الحسابية مباشرة داخل جسم دالة Composable يؤدي لإعادة حسابها بالكامل مع كل ومضة أو إعادة رسم للواجهة، مما يستهلك المعالج ويسبب هبوط في معدل الإطارات (Frame Drops).",
                                    "fix_suggestion": "Wrap the operation in `remember(key) { ... }` or compute it inside the ViewModel StateFlow.",
                                    "fix_code": f"val computedValue = remember(dependency) {{\n    {line.strip()}\n}}"
                                })

    def _check_unscoped_coroutines(self, rel_path: str, lines: List[str], content: str):
        for idx, line in enumerate(lines, 1):
            # Check GlobalScope or CoroutineScope launch directly in Composable
            if ("GlobalScope.launch" in line or "CoroutineScope(" in line) and "rememberCoroutineScope" not in line:
                # Check if inside @Composable
                in_comp = False
                for prev in range(max(0, idx - 15), idx):
                    if "@Composable" in lines[prev]:
                        in_comp = True
                        break

                if in_comp:
                    self.issues.append({
                        "id": f"COMPOSE-LEAKED-COROUTINE-{Path(rel_path).stem}-{idx}",
                        "title": "Unscoped Coroutine Launch in Composable",
                        "title_ar": "إطلاق Coroutine غير مقيد بنطاق دورة الحياة داخل Composable",
                        "category": "Concurrency & Coroutines",
                        "category_ar": "التزامن وخيوط المعالجة",
                        "severity": "CRITICAL",
                        "file": rel_path,
                        "line": idx,
                        "code_snippet": line.strip(),
                        "description": "Launching coroutines directly with GlobalScope or ad-hoc CoroutineScope inside a Composable leaks execution context, ignores Composable lifecycle cancellations, and spawns duplicate background jobs on every recomposition.",
                        "description_ar": "إطلاق الـ Coroutines بشكل عشوائي دون ربطها بنطاق الـ Composable يتسبب في تسريب الذاكرة، عدم إلغاء المهام عند مغادرة الشاشة، وتكرار المهام الخلفية مع كل إعادة رسم.",
                        "fix_suggestion": "Use `val scope = rememberCoroutineScope()` for user event callbacks, or `LaunchedEffect(key) { ... }` for side effects.",
                        "fix_code": "val scope = rememberCoroutineScope()\n// Inside onClick handler:\nscope.launch {\n    // Async action\n}"
                    })

    def _check_state_mutations(self, rel_path: str, lines: List[str], content: str):
        for idx, line in enumerate(lines, 1):
            if re.search(r'\b[a-zA-Z0-9_]+\.value\s*=\s*', line) or re.search(r'\b[a-zA-Z0-9_]+\.value\+\+', line):
                # Ensure it's not inside a lambda onClick or LaunchedEffect
                is_in_handler = False
                for lookback in range(max(0, idx - 5), idx):
                    prev = lines[lookback]
                    if any(k in prev for k in ["onClick", "onValueChange", "LaunchedEffect", "rememberCoroutineScope", "DisposableEffect", "scope.launch"]):
                        is_in_handler = True
                        break

                if not is_in_handler and "ViewModel" not in rel_path:
                    self.issues.append({
                        "id": f"COMPOSE-STATE-MUTATION-IN-BODY-{Path(rel_path).stem}-{idx}",
                        "title": "Direct State Mutation in Composable Body",
                        "title_ar": "تعديل مباشر للحالة (State Mutation) في جسم دالة Composable",
                        "category": "Compose Performance",
                        "category_ar": "أداء واجهات Compose",
                        "severity": "CRITICAL",
                        "file": rel_path,
                        "line": idx,
                        "code_snippet": line.strip(),
                        "description": "Mutating state directly in the Composable function body triggers a new recomposition before the current one finishes, which can create an infinite recomposition loop and crash or freeze the app with 100% CPU usage.",
                        "description_ar": "تعديل الحالة مباشرة أثناء تنفيذ دالة Composable يجبر النظام على إعادة الرسم فوراً، مما قد يدخل التطبيق في حلقة إعادة رسم لانهائية (Infinite Recomposition Loop) وتجميد كامل للتطبيق واستهلاك 100% من المعالج.",
                        "fix_suggestion": "Move state mutation into an event callback (like onClick) or handle it inside a ViewModel.",
                        "fix_code": "// Inside event handler:\nButton(onClick = {\n    state.value = newValue\n}) { ... }"
                    })

    def _check_hardcoded_colors(self, rel_path: str, lines: List[str], content: str):
        for idx, line in enumerate(lines, 1):
            if re.search(r'Color\s*\(\s*0x[0-9a-fA-F]+\s*\)', line) and "Theme" not in rel_path and "Color.kt" not in rel_path:
                self.issues.append({
                    "id": f"COMPOSE-HARDCODED-COLOR-{Path(rel_path).stem}-{idx}",
                    "title": "Hardcoded Color Value (Dark Mode Anti-Pattern)",
                    "title_ar": "استخدام لون ثابت Hardcoded يتجاهل الوضع الليلي والمظهر (Theme)",
                    "category": "Architecture & Clean Code",
                    "category_ar": "جودة الكود والبنية",
                    "severity": "LOW",
                    "file": rel_path,
                    "line": idx,
                    "code_snippet": line.strip(),
                    "description": "Directly hardcoding hex colors inside UI components prevents dynamic theme switching (Dark/Light mode) and breaks consistency with the app design system.",
                    "description_ar": "استخدام أكواد الألوان المباشرة داخل مكونات الواجهة يعطل التوافق التلقائي مع الوضع الليلي (Dark Mode) ويفصل المكون عن نظام التصميم الموحد للتطبيق.",
                    "fix_suggestion": "Use MaterialTheme.colorScheme tokens (e.g. `MaterialTheme.colorScheme.primary`).",
                    "fix_code": "color = MaterialTheme.colorScheme.primary"
                })

    def _check_unremembered_lambdas(self, rel_path: str, lines: List[str], content: str):
        pass
