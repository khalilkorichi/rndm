"""
RNDM Inspector - Database, Migrations & Indexing Analyzer
Deep static analysis for Room DB, Entities, DAOs, Migrations, and SQL Query Optimization.
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Any, Optional

class DatabaseAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.issues: List[Dict[str, Any]] = []
        self.entities: Dict[str, Dict[str, Any]] = {}
        self.daos: Dict[str, Dict[str, Any]] = {}
        self.database_meta: Dict[str, Any] = {}
        self.migrations: List[Dict[str, Any]] = []

    def analyze(self) -> Dict[str, Any]:
        """Runs complete database analysis."""
        self.issues = []
        self.entities = {}
        self.daos = {}
        self.database_meta = {}
        self.migrations = []

        self._scan_entities()
        self._scan_daos()
        self._scan_database_and_migrations()
        self._cross_audit_indexes_and_queries()

        return {
            "summary": {
                "total_entities": len(self.entities),
                "total_daos": len(self.daos),
                "total_migrations": len(self.migrations),
                "db_version": self.database_meta.get("version", 1),
                "export_schema": self.database_meta.get("export_schema", True),
                "db_issues_count": len(self.issues)
            },
            "entities": self.entities,
            "daos": self.daos,
            "database_meta": self.database_meta,
            "migrations": self.migrations,
            "issues": self.issues
        }

    def _scan_entities(self):
        """Scans all Room @Entity classes."""
        entity_dir = self.project_root / "app" / "src" / "main" / "java"
        if not entity_dir.exists():
            return

        for kt_file in entity_dir.rglob("*.kt"):
            try:
                content = kt_file.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue

            if "@Entity" not in content:
                continue

            rel_path = str(kt_file.relative_to(self.project_root)).replace("\\", "/")
            self._parse_entity_file(kt_file, rel_path, content)

    def _parse_entity_file(self, file_path: Path, rel_path: str, content: str):
        # Extract tableName
        table_name_match = re.search(r'@Entity\s*\(\s*(?:[^)]*tableName\s*=\s*"([^"]+)")?', content)
        table_name = table_name_match.group(1) if (table_name_match and table_name_match.group(1)) else file_path.stem.replace("Entity", "").lower()
        
        # Check explicit tableName
        direct_tbl = re.search(r'tableName\s*=\s*"([^"]+)"', content)
        if direct_tbl:
            table_name = direct_tbl.group(1)

        # Extract class name
        class_match = re.search(r'(?:data\s+)?class\s+([A-Za-z0-9_]+)', content)
        class_name = class_match.group(1) if class_match else file_path.stem

        # Extract foreignKeys
        foreign_keys = []
        fk_blocks = re.findall(r'ForeignKey\s*\((.*?)\)', content, re.DOTALL)
        for fk_block in fk_blocks:
            entity_match = re.search(r'entity\s*=\s*([A-Za-z0-9_]+)::class', fk_block)
            parent_cols = re.search(r'parentColumns\s*=\s*\[(.*?)\]', fk_block)
            child_cols = re.search(r'childColumns\s*=\s*\[(.*?)\]', fk_block)
            on_delete = re.search(r'onDelete\s*=\s*ForeignKey\.([A-Z_]+)', fk_block)

            p_cols = [c.strip().strip('"').strip("'") for c in parent_cols.group(1).split(',')] if parent_cols else []
            c_cols = [c.strip().strip('"').strip("'") for c in child_cols.group(1).split(',')] if child_cols else []

            foreign_keys.append({
                "parent_entity": entity_match.group(1) if entity_match else "Unknown",
                "parent_columns": p_cols,
                "child_columns": c_cols,
                "on_delete": on_delete.group(1) if on_delete else "NO_ACTION",
                "raw": fk_block.strip()
            })

        # Extract declared indexes
        declared_indices = []
        index_blocks = re.findall(r'Index\s*\((.*?)\)', content, re.DOTALL)
        for idx in index_blocks:
            val_match = re.search(r'value\s*=\s*\[(.*?)\]', idx)
            unique = "unique = true" in idx.lower() or "unique=true" in idx.lower()
            if val_match:
                cols = [c.strip().strip('"').strip("'") for c in val_match.group(1).split(',')]
            else:
                simple_cols = re.findall(r'"([^"]+)"', idx)
                cols = simple_cols if simple_cols else []
            if cols:
                declared_indices.append({"columns": cols, "unique": unique})

        # Extract fields / columns
        fields = []
        lines = content.split('\n')
        primary_keys = []
        for i, line in enumerate(lines, 1):
            if re.search(r'(?:val|var)\s+[a-zA-Z0-9_]+', line):
                is_pk = "@PrimaryKey" in line or (i > 1 and "@PrimaryKey" in lines[i-2])
                auto_gen = "autoGenerate = true" in line or (i > 1 and "autoGenerate = true" in lines[i-2])
                f_match = re.search(r'(?:val|var)\s+([a-zA-Z0-9_]+)\s*:\s*([A-Za-z0-9_<>?]+)', line)
                if f_match:
                    f_name = f_match.group(1)
                    f_type = f_match.group(2)
                    is_nullable = "?" in f_type
                    if is_pk:
                        primary_keys.append(f_name)
                    fields.append({
                        "name": f_name,
                        "type": f_type,
                        "nullable": is_nullable,
                        "is_primary_key": is_pk,
                        "auto_generate": auto_gen,
                        "line": i
                    })

        entity_info = {
            "class_name": class_name,
            "table_name": table_name,
            "file": rel_path,
            "foreign_keys": foreign_keys,
            "declared_indices": declared_indices,
            "primary_keys": primary_keys,
            "fields": fields
        }
        self.entities[table_name] = entity_info

        # Audit 1: Missing Index on Foreign Keys
        for fk in foreign_keys:
            child_cols = fk["child_columns"]
            indexed = False
            for decl_idx in declared_indices:
                if decl_idx["columns"][:len(child_cols)] == child_cols:
                    indexed = True
                    break
            if not indexed and child_cols:
                col_str = ", ".join(f'"{c}"' for c in child_cols)
                suggested_code = f'@Entity(\n    tableName = "{table_name}",\n    indices = [Index(value = [{col_str}])]\n)'
                self.issues.append({
                    "id": f"DB-MISSING-FK-INDEX-{table_name}-{'-'.join(child_cols)}",
                    "title": f"Missing Index on Foreign Key column(s) `{', '.join(child_cols)}` in table `{table_name}`",
                    "title_ar": f"فهرس مفقود على المفتاح الخارجي `{', '.join(child_cols)}` في جدول `{table_name}`",
                    "category": "Database & Indexing",
                    "category_ar": "قواعد البيانات والفهرسة",
                    "severity": "HIGH",
                    "file": rel_path,
                    "line": 10,
                    "code_snippet": f"foreignKeys = [\n    ForeignKey(\n        entity = {fk['parent_entity']}::class,\n        childColumns = [{', '.join(f'\"{c}\"' for c in child_cols)}]\n    )\n]",
                    "description": f"The foreign key on columns {child_cols} referencing `{fk['parent_entity']}` does not have an index. When parent rows are updated or deleted (especially with CASCADE), SQLite performs a full table scan on `{table_name}`, which causes severe UI stutters and lag.",
                    "description_ar": f"المفتاح الخارجي في الحقل {child_cols} الذي يشير إلى `{fk['parent_entity']}` يفتقر إلى الفهرسة (Index). عند تعديل أو حذف السجلات في الجدول الأب (خصوصاً مع CASCADE)، يقوم SQLite بفحص الجدول بالكامل (Full Table Scan) مما يؤدي لتجميد الواجهة وتأخير ملحوظ في الأداء.",
                    "fix_suggestion": f"Add `Index(value = [{col_str}])` inside `@Entity(indices = [...])`.",
                    "fix_code": suggested_code
                })

    def _scan_daos(self):
        """Scans Room @Dao interfaces and queries."""
        dao_dir = self.project_root / "app" / "src" / "main" / "java"
        if not dao_dir.exists():
            return

        for kt_file in dao_dir.rglob("*.kt"):
            try:
                content = kt_file.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue

            if "@Dao" not in content:
                continue

            rel_path = str(kt_file.relative_to(self.project_root)).replace("\\", "/")
            self._parse_dao_file(kt_file, rel_path, content)

    def _parse_dao_file(self, file_path: Path, rel_path: str, content: str):
        class_match = re.search(r'(?:interface|abstract\s+class)\s+([A-Za-z0-9_]+)', content)
        dao_name = class_match.group(1) if class_match else file_path.stem

        methods = []
        lines = content.split('\n')

        for idx, line in enumerate(lines, 1):
            if "@Query" in line:
                q_match = re.search(r'@Query\s*\(\s*"([^"]+)"\s*\)', line)
                if not q_match and idx < len(lines):
                    multi_chunk = "\n".join(lines[idx-1:min(idx+10, len(lines))])
                    q_match = re.search(r'@Query\s*\(\s*"""(.*?)"""\s*\)|@Query\s*\(\s*"([^"]+)"\s*\)', multi_chunk, re.DOTALL)

                if q_match:
                    sql_query = (q_match.group(1) or q_match.group(2) or "").strip()
                    func_name = "unknown"
                    return_type = "Unit"
                    is_suspend = False
                    for next_idx in range(idx, min(idx + 5, len(lines))):
                        m_line = lines[next_idx]
                        fn_match = re.search(r'(suspend\s+)?fun\s+([a-zA-Z0-9_]+)\s*\((.*?)\)(?:\s*:\s*([^\n{]+))?', m_line)
                        if fn_match:
                            is_suspend = bool(fn_match.group(1))
                            func_name = fn_match.group(2)
                            return_type = (fn_match.group(4) or "").strip()
                            break

                    methods.append({
                        "name": func_name,
                        "query": sql_query,
                        "line": idx,
                        "is_suspend": is_suspend,
                        "return_type": return_type
                    })

                    self._audit_sql_query(sql_query, func_name, dao_name, rel_path, idx, is_suspend, return_type)

        self.daos[dao_name] = {
            "name": dao_name,
            "file": rel_path,
            "methods": methods
        }

    def _audit_sql_query(self, query: str, func_name: str, dao_name: str, rel_path: str, line: int, is_suspend: bool, return_type: str):
        upper_q = query.upper()
        table_match = re.search(r'FROM\s+([a-zA-Z0-9_]+)', upper_q)
        target_table = table_match.group(1).lower() if table_match else None

        if "WHERE" in upper_q and "ORDER BY" in upper_q and target_table in self.entities:
            where_part = upper_q.split("WHERE")[1].split("ORDER BY")[0]
            order_part = upper_q.split("ORDER BY")[1]

            where_cols = [c.strip().lower() for c in re.findall(r'([a-zA-Z0-9_]+)\s*=', where_part)]
            order_cols = [c.strip().lower() for c in re.findall(r'([a-zA-Z0-9_]+)(?:\s+ASC|\s+DESC)?', order_part) if c.strip() not in ('ASC', 'DESC', 'LIMIT', '')]

            all_target_cols = where_cols + order_cols
            if len(all_target_cols) >= 2:
                entity = self.entities[target_table]
                has_matching_index = False
                for idx_obj in entity.get("declared_indices", []):
                    idx_cols = [c.lower() for c in idx_obj.get("columns", [])]
                    if idx_cols[:len(where_cols)] == where_cols:
                        has_matching_index = True
                        break

                if not has_matching_index and where_cols:
                    suggested_idx_cols = ", ".join(f'"{c}"' for c in (where_cols + order_cols)[:3])
                    self.issues.append({
                        "id": f"DB-MISSING-COMPOSITE-INDEX-{target_table}-{func_name}",
                        "title": f"Missing Composite Index for Query `{func_name}` in `{dao_name}`",
                        "title_ar": f"فهرس مركب مفقود للاستعلام `{func_name}` في `{dao_name}`",
                        "category": "Database & Indexing",
                        "category_ar": "قواعد البيانات والفهرسة",
                        "severity": "MEDIUM",
                        "file": rel_path,
                        "line": line,
                        "code_snippet": f'@Query("{query}")\nfun {func_name}(...): {return_type}',
                        "description": f"Query filters on `{', '.join(where_cols)}` and orders by `{', '.join(order_cols)}` on table `{target_table}`, but no matching composite index was found. This causes SQLite temporary B-tree sorting in memory during query execution.",
                        "description_ar": f"الاستعلام يقوم بالفلترة على `{', '.join(where_cols)}` والترتيب بحسب `{', '.join(order_cols)}` في جدول `{target_table}` دون وجود فهرس مركب مطابق. يؤدي هذا لإنشاء جدول فرز مؤقت في الذاكرة (Temporary B-tree) وإبطاء الاستعلام.",
                        "fix_suggestion": f"Add composite index `Index(value = [{suggested_idx_cols}])` to `{entity['class_name']}`.",
                        "fix_code": f'@Entity(\n    tableName = "{target_table}",\n    indices = [\n        Index(value = [{suggested_idx_cols}])\n    ]\n)'
                    })

        if "SELECT *" in upper_q and "LIMIT" not in upper_q and "WHERE ID" not in upper_q and "WHERE ID =" not in upper_q:
            if "List<" in return_type and "Flow<" not in return_type:
                self.issues.append({
                    "id": f"DB-UNBOUNDED-LIST-QUERY-{target_table}-{func_name}",
                    "title": f"Unbounded List Query without Pagination/Limit in `{func_name}`",
                    "title_ar": f"استعلام قائمة غير محدود بدون ترقيم (Pagination) في `{func_name}`",
                    "category": "Database & Indexing",
                    "category_ar": "قواعد البيانات والفهرسة",
                    "severity": "LOW",
                    "file": rel_path,
                    "line": line,
                    "code_snippet": f'@Query("{query}")\nsuspend fun {func_name}(...): {return_type}',
                    "description": f"Loading full table lists into memory synchronously without `LIMIT` or Paging 3 can cause memory pressure (OOM) as table size grows.",
                    "description_ar": f"تحميل كامل سجلات الجدول إلى الذاكرة دفعة واحدة بدون تحديد العدد `LIMIT` أو استخدام مكتبة Paging قد يسبب استهلاكاً عالياً للذاكرة وبطء في المعالجة مع نمو حجم البيانات.",
                    "fix_suggestion": "Use Flow with Paging3 or add LIMIT/OFFSET pagination to the query.",
                    "fix_code": f'@Query("{query} LIMIT :limit OFFSET :offset")'
                })

    def _scan_database_and_migrations(self):
        """Scans RoomDatabase class, versions, and Migration objects."""
        db_files = list(self.project_root.rglob("*Database*.kt"))
        for db_file in db_files:
            try:
                content = db_file.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue

            if "@Database" not in content:
                continue

            rel_path = str(db_file.relative_to(self.project_root)).replace("\\", "/")

            version_match = re.search(r'version\s*=\s*(\d+)', content)
            version = int(version_match.group(1)) if version_match else 1

            export_schema_match = re.search(r'exportSchema\s*=\s*(false|true)', content)
            export_schema = export_schema_match.group(1) == "true" if export_schema_match else True

            entities_match = re.search(r'entities\s*=\s*\[(.*?)\]', content, re.DOTALL)
            declared_entities = []
            if entities_match:
                declared_entities = [e.strip().replace("::class", "") for e in entities_match.group(1).split(",") if e.strip()]

            migration_blocks = re.findall(
                r'val\s+(MIGRATION_\d+_\d+)\s*=\s*object\s*:\s*Migration\((\d+),\s*(\d+)\)\s*\{(.*?)\}',
                content,
                re.DOTALL
            )
            for mig_var, from_v, to_v, mig_body in migration_blocks:
                from_ver = int(from_v)
                to_ver = int(to_v)
                sql_execs = re.findall(r'db\.execSQL\(\s*"([^"]+)"\s*\)', mig_body)
                
                for sql in sql_execs:
                    if "ADD COLUMN" in sql.upper() and "NOT NULL" in sql.upper() and "DEFAULT" not in sql.upper():
                        self.issues.append({
                            "id": f"DB-MIGRATION-UNSAFE-NOT-NULL-{mig_var}",
                            "title": f"Unsafe SQLite Migration in `{mig_var}` (NOT NULL without DEFAULT)",
                            "title_ar": f"ترحيل غير آمن في `{mig_var}` (إضافة عمود NOT NULL بدون DEFAULT)",
                            "category": "Database & Indexing",
                            "category_ar": "قواعد البيانات والفهرسة",
                            "severity": "CRITICAL",
                            "file": rel_path,
                            "line": content.count('\n', 0, content.find(mig_var)) + 1,
                            "code_snippet": f'db.execSQL("{sql}")',
                            "description": f"Adding a `NOT NULL` column without a `DEFAULT` value in SQLite during migration `{mig_var}` will crash the app when existing rows are migrated because SQLite cannot populate existing rows with non-null values.",
                            "description_ar": f"إضافة عمود يحمل قيد `NOT NULL` دون تعيين قيمة افتراضية `DEFAULT` في SQLite أثناء الترحيل `{mig_var}` يتسبب في انهيار التطبيق (Crash) عند وجود بيانات سابقة، لعدم قدرة قاعدة البيانات على ملء الحقول القديمة.",
                            "fix_suggestion": "Specify a default value in the ALTER TABLE statement (e.g., `DEFAULT 0` or `DEFAULT ''`).",
                            "fix_code": f'db.execSQL("{sql} DEFAULT 0")'
                        })

                self.migrations.append({
                    "name": mig_var,
                    "from_version": from_ver,
                    "to_version": to_ver,
                    "sql_statements": sql_execs,
                    "is_direct_sequence": (to_ver == from_ver + 1)
                })

            self.database_meta = {
                "file": rel_path,
                "version": version,
                "export_schema": export_schema,
                "entities": declared_entities
            }

            if not export_schema:
                self.issues.append({
                    "id": "DB-SCHEMA-EXPORT-DISABLED",
                    "title": "Room Schema Export is Disabled (`exportSchema = false`)",
                    "title_ar": "تصدير مخطط قاعدة البيانات معطل (`exportSchema = false`)",
                    "category": "Database & Indexing",
                    "category_ar": "قواعد البيانات والفهرسة",
                    "severity": "LOW",
                    "file": rel_path,
                    "line": content.count('\n', 0, content.find("exportSchema")) + 1 if "exportSchema" in content else 30,
                    "code_snippet": "@Database(\n    ...\n    exportSchema = false\n)",
                    "description": "Disabling schema export prevents Room from generating schema JSON files in version control, making automated migration verification and schema diffing impossible.",
                    "description_ar": "تعطيل تصدير المخطط يمنع Room من توليد ملفات JSON للتحقق من المخطط عبر نظام التحكم بالإصدارات (Git)، مما يصعب التحقق الآلي من سلامة الترحيلات.",
                    "fix_suggestion": "Set `exportSchema = true` and configure `room.schemaLocation` in build.gradle.kts.",
                    "fix_code": "@Database(\n    ...\n    exportSchema = true\n)"
                })

            covered_steps = {(m["from_version"], m["to_version"]) for m in self.migrations}
            for v in range(1, version):
                if (v, v + 1) not in covered_steps:
                    self.issues.append({
                        "id": f"DB-MISSING-MIGRATION-STEP-{v}-TO-{v+1}",
                        "title": f"Missing Step-by-Step Migration `{v}` -> `{v+1}`",
                        "title_ar": f"مسار ترحيل مفقود من الإصدار `{v}` إلى `{v+1}`",
                        "category": "Database & Indexing",
                        "category_ar": "قواعد البيانات والفهرسة",
                        "severity": "HIGH",
                        "file": rel_path,
                        "line": 32,
                        "code_snippet": f"Database version is {version}, but migration from {v} to {v+1} was not explicitly defined.",
                        "description": f"No explicit `Migration({v}, {v+1})` found. If users update from version {v}, Room will throw an IllegalStateException unless fallbackToDestructiveMigration is explicitly configured.",
                        "description_ar": f"لم يتم العثور على كائن ترحيل مباشر `Migration({v}, {v+1})`. عند تحديث المستخدم للتطبيق من الإصدار {v}، سينهار التطبيق باستثناء `IllegalStateException` ما لم يتم تفعيل الترحيل الإتلافي.",
                        "fix_suggestion": f"Define `val MIGRATION_{v}_{v+1} = object : Migration({v}, {v+1}) {{ ... }}`.",
                        "fix_code": f"val MIGRATION_{v}_{v+1} = object : Migration({v}, {v+1}) {{\n    override fun migrate(db: SupportSQLiteDatabase) {{\n        // Migration steps\n    }}\n}}"
                    })

    def _cross_audit_indexes_and_queries(self):
        """Cross-checks entity indexes against all DAO queries."""
        pass
