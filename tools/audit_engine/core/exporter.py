"""
RNDM Inspector - Exporter & Report Generator
Generates Markdown reports, AI-fix prompts, and clipboard snippets.
"""

from typing import Dict, List, Any

class ReportExporter:
    @staticmethod
    def format_single_issue_markdown(issue: Dict[str, Any]) -> str:
        """Formats a single issue as rich markdown for copying or feeding to AI."""
        return f"""### 🚨 [{issue.get('severity', 'MEDIUM')}] {issue.get('title', 'Unknown Issue')}
**التصنيف / Category:** {issue.get('category_ar', issue.get('category', ''))}
**الملف المتضرر / File:** `{issue.get('file', '')}:{issue.get('line', 1)}`

#### 📋 وصف الخلل وتأثيره على التطبيق (Defect & Impact):
{issue.get('description_ar', '')}
> {issue.get('description', '')}

#### 💻 مقتطف الكود الحالي (Current Code Snippet):
```kotlin
{issue.get('code_snippet', '')}
```

#### 🛠️ الحل المقترح وكود التصحيح (Recommended Fix):
**التوجيه:** {issue.get('fix_suggestion', '')}
```kotlin
{issue.get('fix_code', '')}
```
"""

    @staticmethod
    def generate_full_markdown_report(scan_data: Dict[str, Any]) -> str:
        """Generates a full Markdown audit report."""
        summary = scan_data.get("summary", {})
        health = scan_data.get("health", {})
        issues = scan_data.get("issues", [])
        db_summary = scan_data.get("database_summary", {})

        md = []
        md.append("# 📊 تقرير الفحص والتدقيق الشامل لتطبيق RNDM")
        md.append(f"**تاريخ الفحص:** {scan_data.get('date_str', '')}")
        md.append(f"**التقييم العام:** {health.get('grade', 'N/A')} ({health.get('score', 0)}/100) - {health.get('status_ar', '')}\n")

        md.append("## 📈 ملخص الإحصائيات (Audit Summary)")
        md.append(f"- **إجمالي المشاكل المكتشفة:** {summary.get('total', 0)}")
        md.append(f"- **حرجة (Critical):** {summary.get('critical', 0)}")
        md.append(f"- **عالية (High):** {summary.get('high', 0)}")
        md.append(f"- **متوسطة (Medium):** {summary.get('medium', 0)}")
        md.append(f"- **منخفضة (Low):** {summary.get('low', 0)}")
        md.append(f"- **تم حلها منذ آخر فحص:** {summary.get('resolved_since_last', 0)}\n")

        md.append("## 🗄️ ملخص قواعد البيانات والترحيل (Database Overview)")
        md.append(f"- **إصدار قاعدة البيانات (Version):** {db_summary.get('db_version', 1)}")
        md.append(f"- **عدد الكيانات والجداول (Entities):** {db_summary.get('total_entities', 0)}")
        md.append(f"- **عدد ملفات الـ DAOs:** {db_summary.get('total_daos', 0)}")
        md.append(f"- **عدد خطوات الترحيل (Migrations):** {db_summary.get('total_migrations', 0)}")
        md.append(f"- **تصدير المخطط (Schema Export):** {'مفعل' if db_summary.get('export_schema') else 'معطل'}\n")

        md.append("## 🔍 تفاصيل المشاكل المكتشفة (Detailed Issues List)\n")
        for idx, issue in enumerate(issues, 1):
            md.append(f"---")
            md.append(f"### {idx}. [{issue.get('severity')}] {issue.get('title')}")
            md.append(f"- **الملف:** `{issue.get('file')}:{issue.get('line')}`")
            md.append(f"- **التصنيف:** {issue.get('category_ar')} ({issue.get('category')})")
            md.append(f"\n**الوصف والتأثير:**\n{issue.get('description_ar')}\n")
            md.append(f"**الكود الحالي:**\n```kotlin\n{issue.get('code_snippet')}\n```\n")
            md.append(f"**الحل المقترح:**\n```kotlin\n{issue.get('fix_code')}\n```\n")

        return "\n".join(md)
