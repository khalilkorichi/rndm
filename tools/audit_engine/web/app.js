/**
 * RNDM Inspector - Interactive Web Application Engine
 * Handles real-time scanning, filtering, database inspection, and clipboard exports.
 */

// Application State
let appState = {
  scanData: null,
  filteredIssues: [],
  filters: {
    search: '',
    severity: 'ALL',
    category: 'ALL',
    file: 'ALL'
  },
  theme: localStorage.getItem('rndm_theme') || 'dark'
};

// DOM Elements
const elements = {
  btnRunScan: document.getElementById('btnRunScan'),
  scanBtnIcon: document.getElementById('scanBtnIcon'),
  btnThemeToggle: document.getElementById('btnThemeToggle'),
  loadingOverlay: document.getElementById('loadingOverlay'),
  toastNotification: document.getElementById('toastNotification'),
  toastMessage: document.getElementById('toastMessage'),
  
  // Header Elements
  headerScoreVal: document.getElementById('headerScoreVal'),
  headerGradeRing: document.getElementById('headerGradeRing'),
  
  // Dashboard Metrics
  healthScoreNum: document.getElementById('healthScoreNum'),
  healthScoreDesc: document.getElementById('healthScoreDesc'),
  dashboardGradeBox: document.getElementById('dashboardGradeBox'),
  metricCriticalCount: document.getElementById('metricCriticalCount'),
  metricHighCount: document.getElementById('metricHighCount'),
  metricResolvedCount: document.getElementById('metricResolvedCount'),
  metricFilesCount: document.getElementById('metricFilesCount'),
  metricScanDuration: document.getElementById('metricScanDuration'),
  summaryBannerText: document.getElementById('summaryBannerText'),
  categoryBarsContainer: document.getElementById('categoryBarsContainer'),
  
  // DB Metrics
  statDbVersion: document.getElementById('statDbVersion'),
  statEntitiesCount: document.getElementById('statEntitiesCount'),
  statMissingFkCount: document.getElementById('statMissingFkCount'),
  statMigrationsCount: document.getElementById('statMigrationsCount'),
  statSchemaExportStatus: document.getElementById('statSchemaExportStatus'),
  tabDbBadge: document.getElementById('tabDbBadge'),
  tabCodeBadge: document.getElementById('tabCodeBadge'),
  
  // Database Tab
  dbIssuesSummaryText: document.getElementById('dbIssuesSummaryText'),
  entitiesListContainer: document.getElementById('entitiesListContainer'),
  migrationsTimeline: document.getElementById('migrationsTimeline'),
  daosListContainer: document.getElementById('daosListContainer'),
  subtabEntitiesCount: document.getElementById('subtabEntitiesCount'),
  subtabMigrationsCount: document.getElementById('subtabMigrationsCount'),
  subtabDaosCount: document.getElementById('subtabDaosCount'),
  btnCopyAllDbFixes: document.getElementById('btnCopyAllDbFixes'),
  
  // Code Audit Tab
  searchInput: document.getElementById('searchInput'),
  filterSeverity: document.getElementById('filterSeverity'),
  filterCategory: document.getElementById('filterCategory'),
  filterFile: document.getElementById('filterFile'),
  displayedIssuesCount: document.getElementById('displayedIssuesCount'),
  totalIssuesCount: document.getElementById('totalIssuesCount'),
  issuesListContainer: document.getElementById('issuesListContainer'),
  btnCopyFilteredMarkdown: document.getElementById('btnCopyFilteredMarkdown'),
  
  // Memory & Recomposition Tab
  recomposeWarningsList: document.getElementById('recomposeWarningsList'),
  concurrencyWarningsList: document.getElementById('concurrencyWarningsList'),
  
  // Export Center
  btnCopyFullMarkdown: document.getElementById('btnCopyFullMarkdown'),
  btnDownloadMarkdown: document.getElementById('btnDownloadMarkdown'),
  btnCopyAiPrompt: document.getElementById('btnCopyAiPrompt'),
  btnDownloadJson: document.getElementById('btnDownloadJson'),
  markdownPreviewTextarea: document.getElementById('markdownPreviewTextarea')
};

// Initialization
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  setupEventListeners();
  loadInitialData();
});

// Setup Event Listeners
function setupEventListeners() {
  // Theme Toggle
  elements.btnThemeToggle.addEventListener('click', toggleTheme);

  // Run Scan
  elements.btnRunScan.addEventListener('click', runAuditScan);

  // Main Tabs
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      switchTab(tab.getAttribute('data-tab'));
    });
  });

  // Sub Tabs in Database view
  document.querySelectorAll('.sub-tab').forEach(subtab => {
    subtab.addEventListener('click', () => {
      switchSubTab(subtab.getAttribute('data-subtab'));
    });
  });

  // Search & Filters
  elements.searchInput.addEventListener('input', (e) => {
    appState.filters.search = e.target.value.toLowerCase();
    applyFiltersAndRenderIssues();
  });

  elements.filterSeverity.addEventListener('change', (e) => {
    appState.filters.severity = e.target.value;
    applyFiltersAndRenderIssues();
  });

  elements.filterCategory.addEventListener('change', (e) => {
    appState.filters.category = e.target.value;
    applyFiltersAndRenderIssues();
  });

  elements.filterFile.addEventListener('change', (e) => {
    appState.filters.file = e.target.value;
    applyFiltersAndRenderIssues();
  });

  // Export Buttons
  elements.btnCopyFullMarkdown.addEventListener('click', copyFullMarkdownReport);
  elements.btnDownloadMarkdown.addEventListener('click', downloadMarkdownReport);
  elements.btnCopyAiPrompt.addEventListener('click', copyAiPrompt);
  elements.btnDownloadJson.addEventListener('click', downloadJsonReport);
  elements.btnCopyFilteredMarkdown.addEventListener('click', copyFilteredMarkdownReport);
  elements.btnCopyAllDbFixes.addEventListener('click', copyAllDatabaseFixes);
}

// Initial Data Load
async function loadInitialData() {
  showLoading(true);
  try {
    const res = await fetch('/api/latest');
    if (res.ok) {
      const data = await res.json();
      appState.scanData = data;
      renderAll(data);
    } else {
      await runAuditScan();
    }
  } catch (err) {
    console.warn('Initial load failed, triggering scan:', err);
    await runAuditScan();
  } finally {
    showLoading(false);
  }
}

// Trigger Live Scan
async function runAuditScan() {
  showLoading(true);
  elements.scanBtnIcon.classList.add('fa-spin');
  try {
    const res = await fetch('/api/scan', { method: 'POST' });
    if (!res.ok) throw new Error('Scan failed on server');
    const data = await res.json();
    appState.scanData = data;
    renderAll(data);
    showToast('✨ تم اكتمال فحص وتدقيق التطبيق بنجاح!');
  } catch (err) {
    console.error('Scan error:', err);
    showToast('❌ حدث خطأ أثناء فحص التطبيق: ' + err.message);
  } finally {
    elements.scanBtnIcon.classList.remove('fa-spin');
    showLoading(false);
  }
}

// Master Render Function
function renderAll(data) {
  if (!data) return;

  renderHeaderAndMetrics(data);
  renderCategoryBreakdown(data);
  populateFileFilterOptions(data.issues || []);
  applyFiltersAndRenderIssues();
  renderDatabaseTab(data);
  renderMemoryTab(data);
  updateMarkdownPreview(data);
}

// Render Header & High Level Metrics
function renderHeaderAndMetrics(data) {
  const health = data.health || { score: 100, grade: 'A+', color: '#10B981', status_ar: 'ممتاز' };
  const summary = data.summary || {};
  const diff = data.diff || {};

  // Header Score
  elements.headerScoreVal.textContent = `${health.score}%`;
  elements.headerGradeRing.textContent = health.grade;
  elements.headerGradeRing.style.backgroundColor = health.color;

  // Health Card
  elements.healthScoreNum.textContent = `${health.score}%`;
  elements.healthScoreDesc.textContent = health.status_ar;
  elements.dashboardGradeBox.textContent = health.grade;
  elements.dashboardGradeBox.style.backgroundColor = `${health.color}25`;
  elements.dashboardGradeBox.style.color = health.color;

  // Counts
  elements.metricCriticalCount.textContent = summary.critical || 0;
  elements.metricHighCount.textContent = summary.high || 0;
  elements.metricResolvedCount.textContent = diff.resolved_count || 0;
  elements.metricFilesCount.textContent = data.total_files_scanned || 0;
  elements.metricScanDuration.textContent = `المدة: ${data.duration_ms || 0}ms`;

  // Badges on Tabs
  const criticalAndHigh = (summary.critical || 0) + (summary.high || 0);
  elements.tabCodeBadge.textContent = summary.total || 0;
  
  const dbIssuesCount = (data.issues || []).filter(i => i.category === 'Database & Indexing').length;
  elements.tabDbBadge.textContent = dbIssuesCount;

  // Summary Banner
  if (criticalAndHigh === 0) {
    elements.summaryBannerText.textContent = `الكود في حالة ممتازة! تم فحص جميع الكيانات واستعلامات الـ DAOs وواجهات Compose بدون مشاكل حرجة.`;
  } else {
    elements.summaryBannerText.textContent = `تم اكتشاف ${criticalAndHigh} مشكلة ذات أولوية عالية تتطلب المعالجة (تتعلق بفهرسة قواعد البيانات، إعادة رسم الـ Composables، أو كفاءة الذاكرة).`;
  }
}

// Render Category Bars
function renderCategoryBreakdown(data) {
  const issues = data.issues || [];
  const categories = {
    'Database & Indexing': { name_ar: 'قواعد البيانات والترحيل والفهرسة', color: '#6366F1', count: 0 },
    'Compose Performance': { name_ar: 'أداء واجهات Compose وإعادة الرسم', color: '#F59E0B', count: 0 },
    'Concurrency & Coroutines': { name_ar: 'التزامن والـ Coroutines وخيوط المعالجة', color: '#EF4444', count: 0 },
    'Memory & Leaks': { name_ar: 'الذاكرة وتسريبات الموارد (Leaks)', color: '#A855F7', count: 0 },
    'Architecture & Clean Code': { name_ar: 'جودة الكود والبنية النظيفة', color: '#06B6D4', count: 0 }
  };

  issues.forEach(issue => {
    const cat = issue.category || 'Architecture & Clean Code';
    if (categories[cat]) {
      categories[cat].count++;
    }
  });

  const total = issues.length || 1;
  let html = '';

  for (const [key, info] of Object.entries(categories)) {
    const percent = Math.round((info.count / total) * 100);
    html += `
      <div class="cat-bar-item">
        <div class="cat-bar-header">
          <span>${info.name_ar}</span>
          <span class="font-mono">${info.count} مشكلة (${percent}%)</span>
        </div>
        <div class="cat-bar-track">
          <div class="cat-bar-fill" style="width: ${percent}%; background-color: ${info.color};"></div>
        </div>
      </div>
    `;
  }

  elements.categoryBarsContainer.innerHTML = html;
}

// Populate File Filter
function populateFileFilterOptions(issues) {
  const files = [...new Set(issues.map(i => i.file).filter(Boolean))].sort();
  const currentVal = elements.filterFile.value;

  elements.filterFile.innerHTML = '<option value="ALL">جميع الملفات (All Files)</option>' +
    files.map(f => `<option value="${f}">${f}</option>`).join('');

  if (files.includes(currentVal)) {
    elements.filterFile.value = currentVal;
  }
}

// Filter and Render Issues
function applyFiltersAndRenderIssues() {
  if (!appState.scanData) return;

  const allIssues = appState.scanData.issues || [];
  const { search, severity, category, file } = appState.filters;

  appState.filteredIssues = allIssues.filter(issue => {
    // Severity Filter
    if (severity !== 'ALL' && issue.severity !== severity) return false;

    // Category Filter
    if (category !== 'ALL' && issue.category !== category) return false;

    // File Filter
    if (file !== 'ALL' && issue.file !== file) return false;

    // Search Filter
    if (search) {
      const matchText = [
        issue.title || '',
        issue.title_ar || '',
        issue.file || '',
        issue.description_ar || '',
        issue.code_snippet || ''
      ].join(' ').toLowerCase();
      if (!matchText.includes(search)) return false;
    }

    return true;
  });

  elements.totalIssuesCount.textContent = allIssues.length;
  elements.displayedIssuesCount.textContent = appState.filteredIssues.length;

  renderIssuesList(appState.filteredIssues);
}

// Render Issues List Cards
function renderIssuesList(issues) {
  if (!issues || issues.length === 0) {
    elements.issuesListContainer.innerHTML = `
      <div class="glass-card" style="text-align: center; padding: 48px;">
        <i class="fa-solid fa-circle-check text-success" style="font-size: 42px; margin-bottom: 14px;"></i>
        <h3>لا توجد مشاكل مطابقة لخيارات الفلترة!</h3>
        <p style="color: var(--text-secondary); margin-top: 6px;">الكود سليم تماماً أو قم بتعديل خيارات البحث والفلترة.</p>
      </div>
    `;
    return;
  }

  let html = '';
  issues.forEach((issue, idx) => {
    const sevClass = `badge-${(issue.severity || 'low').toLowerCase()}`;
    const codeSnippet = escapeHtml(issue.code_snippet || '');
    const fixCode = escapeHtml(issue.fix_code || '');

    html += `
      <div class="issue-card" id="issue-card-${idx}">
        <div class="issue-header">
          <div class="issue-title-group">
            <div class="issue-title">${issue.title_ar || issue.title}</div>
            <div class="issue-location">
              <i class="fa-solid fa-file-code"></i>
              <span>${issue.file}:${issue.line || 1}</span>
            </div>
          </div>
          <div class="issue-badges">
            <span class="badge-severity ${sevClass}">${issue.severity}</span>
            <span class="badge-category">${issue.category_ar || issue.category}</span>
          </div>
        </div>

        <div class="issue-body">
          <div class="issue-desc-box">
            <div class="issue-desc-ar"><strong>الخلل والتأثير:</strong> ${issue.description_ar || issue.description}</div>
            ${issue.description ? `<div class="issue-desc-en">${issue.description}</div>` : ''}
          </div>

          <div class="code-preview-container">
            <div class="code-box">
              <div class="code-box-header">
                <span><i class="fa-solid fa-code"></i> الكود الحالي (المتضرر)</span>
                <span class="font-mono">السطر ${issue.line || 1}</span>
              </div>
              <pre><code>${codeSnippet}</code></pre>
            </div>

            <div class="code-box code-fix">
              <div class="code-box-header">
                <span><i class="fa-solid fa-wand-magic-sparkles text-success"></i> الحل والتصحيح المقترح</span>
              </div>
              <pre><code>${fixCode}</code></pre>
            </div>
          </div>
        </div>

        <div class="issue-actions">
          <button class="btn-copy-issue" onclick="copySingleIssue(${idx})">
            <i class="fa-solid fa-copy"></i> نسخ تقرير المشكلة لحلها لاحقاً
          </button>
        </div>
      </div>
    `;
  });

  elements.issuesListContainer.innerHTML = html;
}

// Render Database Tab
function renderDatabaseTab(data) {
  const dbMeta = data.database_details?.database_meta || {};
  const entities = data.database_details?.entities || {};
  const daos = data.database_details?.daos || {};
  const migrations = data.database_details?.migrations || [];
  const issues = data.issues || [];
  const dbIssues = issues.filter(i => i.category === 'Database & Indexing');

  // Quick stats
  elements.statDbVersion.textContent = `v${dbMeta.version || 1}`;
  elements.statEntitiesCount.textContent = Object.keys(entities).length;
  elements.statMigrationsCount.textContent = migrations.length;
  elements.statSchemaExportStatus.textContent = dbMeta.export_schema ? 'مفعل' : 'معطل';

  const missingFkCount = dbIssues.filter(i => i.id.includes('MISSING-FK-INDEX')).length;
  elements.statMissingFkCount.textContent = missingFkCount;

  elements.subtabEntitiesCount.textContent = Object.keys(entities).length;
  elements.subtabMigrationsCount.textContent = migrations.length;
  elements.subtabDaosCount.textContent = Object.keys(daos).length;

  elements.dbIssuesSummaryText.textContent = `تم العثور على ${dbIssues.length} تنبيه في قواعد البيانات (منها ${missingFkCount} مفاتيح خارجية تفتقر للفهرسة).`;

  // 1. Render Entities Cards
  let entitiesHtml = '';
  for (const [tblName, entity] of Object.entries(entities)) {
    const fks = entity.foreign_keys || [];
    const declaredIdx = entity.declared_indices || [];

    entitiesHtml += `
      <div class="entity-card">
        <div class="entity-card-header">
          <div>
            <div class="entity-table-name"><i class="fa-solid fa-table"></i> ${tblName}</div>
            <div class="entity-class-name">${entity.class_name} (${entity.file})</div>
          </div>
          <span class="badge-category">${(entity.fields || []).length} حقول</span>
        </div>

        <div class="entity-indices-list">
          <div style="font-size: 12px; font-weight: 700; color: var(--text-secondary);">الفهارس المسجلة (Indices):</div>
          ${declaredIdx.length > 0 ? declaredIdx.map(idx => `
            <div class="index-pill">
              <i class="fa-solid fa-check"></i>
              <span>Index(${idx.columns.join(', ')})</span>
              ${idx.unique ? '<span class="badge-version">UNIQUE</span>' : ''}
            </div>
          `).join('') : '<div style="font-size: 12px; color: var(--text-muted);">لا توجد فهارس معرفة</div>'}

          ${fks.map(fk => {
            const hasIdx = declaredIdx.some(d => d.columns[0] === fk.child_columns[0]);
            return `
              <div class="index-pill ${hasIdx ? '' : 'missing'}">
                <i class="fa-solid ${hasIdx ? 'fa-key' : 'fa-triangle-exclamation'}"></i>
                <span>FK -> ${fk.parent_entity}(${fk.child_columns.join(', ')})</span>
                ${!hasIdx ? '<span class="text-danger font-mono" style="font-weight: 700;">[مفقود Index!]</span>' : ''}
              </div>
            `;
          }).join('')}
        </div>

        <div style="margin-top: 14px; display: flex; justify-content: flex-end;">
          <button class="btn-secondary-sm" onclick="copyEntityIndexFix('${tblName}')">
            <i class="fa-solid fa-copy"></i> نسخ كود @Index المقترح
          </button>
        </div>
      </div>
    `;
  }
  elements.entitiesListContainer.innerHTML = entitiesHtml || '<p>لا توجد كيانات مسجلة.</p>';

  // 2. Render Migrations Timeline
  let migHtml = '';
  if (migrations.length === 0) {
    migHtml = '<p style="color: var(--text-secondary);">لا توجد ترحيلات مخصصة (قاعدة البيانات في الإصدار 1 أو تعتمد على ترحيل إتلافي).</p>';
  } else {
    migrations.forEach(mig => {
      migHtml += `
        <div class="timeline-step">
          <div class="timeline-header">
            <span class="timeline-version-badge"><i class="fa-solid fa-code-branch"></i> ${mig.name} (الإصدار ${mig.from_version} ➔ ${mig.to_version})</span>
            <span class="badge-category font-mono">${(mig.sql_statements || []).length} SQL Statements</span>
          </div>
          ${(mig.sql_statements || []).map(sql => `
            <div class="code-box">
              <pre><code>${escapeHtml(sql)}</code></pre>
            </div>
          `).join('')}
        </div>
      `;
    });
  }
  elements.migrationsTimeline.innerHTML = migHtml;

  // 3. Render DAOs
  let daosHtml = '';
  for (const [daoName, dao] of Object.entries(daos)) {
    daosHtml += `
      <div style="margin-bottom: 20px;">
        <h4 style="margin-bottom: 10px; color: var(--accent-primary); font-size: 15px;">
          <i class="fa-solid fa-terminal"></i> ${daoName} (${dao.file})
        </h4>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          ${(dao.methods || []).map(m => `
            <div class="stat-row" style="flex-direction: column; align-items: flex-start; gap: 6px;">
              <div style="display: flex; justify-content: space-between; width: 100%;">
                <span class="font-mono" style="font-weight: 700; color: var(--text-primary);">${m.is_suspend ? 'suspend ' : ''}fun ${m.name}(...): ${escapeHtml(m.return_type || '')}</span>
                <span class="font-mono" style="font-size: 11px; color: var(--text-muted);">السطر ${m.line}</span>
              </div>
              <div class="code-box" style="width: 100%;">
                <pre><code>${escapeHtml(m.query)}</code></pre>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }
  elements.daosListContainer.innerHTML = daosHtml || '<p>لا توجد DAOs معرفة.</p>';
}

// Render Memory & Recomposition Tab
function renderMemoryTab(data) {
  const issues = data.issues || [];
  const composeIssues = issues.filter(i => i.category === 'Compose Performance');
  const concurrencyIssues = issues.filter(i => i.category === 'Concurrency & Coroutines' || i.category === 'Memory & Leaks');

  let compHtml = '';
  if (composeIssues.length === 0) {
    compHtml = '<p style="color: var(--accent-emerald);">واجهات Compose متوافقة تماماً ولا تعاني من مشاكل إعادة رسم مكتشفة.</p>';
  } else {
    composeIssues.forEach((issue, idx) => {
      compHtml += `
        <div class="stat-row" style="flex-direction: column; align-items: flex-start; gap: 6px; margin-bottom: 10px;">
          <div style="display: flex; justify-content: space-between; width: 100%;">
            <span style="font-weight: 700; color: var(--severity-high);">${issue.title_ar || issue.title}</span>
            <span class="font-mono" style="font-size: 11px; color: var(--accent-cyan);">${issue.file}:${issue.line}</span>
          </div>
          <div style="font-size: 12.5px; color: var(--text-secondary);">${issue.description_ar}</div>
          <div class="code-box code-fix" style="width: 100%; margin-top: 4px;">
            <pre><code>${escapeHtml(issue.fix_code || '')}</code></pre>
          </div>
        </div>
      `;
    });
  }
  elements.recomposeWarningsList.innerHTML = compHtml;

  let concHtml = '';
  if (concurrencyIssues.length === 0) {
    concHtml = '<p style="color: var(--accent-emerald);">مسارات الـ Coroutines والذاكرة سليمة ولا توجد تسريبات context أو حجب لخيوط المعالجة.</p>';
  } else {
    concurrencyIssues.forEach((issue, idx) => {
      concHtml += `
        <div class="stat-row" style="flex-direction: column; align-items: flex-start; gap: 6px; margin-bottom: 10px;">
          <div style="display: flex; justify-content: space-between; width: 100%;">
            <span style="font-weight: 700; color: var(--severity-critical);">${issue.title_ar || issue.title}</span>
            <span class="font-mono" style="font-size: 11px; color: var(--accent-cyan);">${issue.file}:${issue.line}</span>
          </div>
          <div style="font-size: 12.5px; color: var(--text-secondary);">${issue.description_ar}</div>
          <div class="code-box code-fix" style="width: 100%; margin-top: 4px;">
            <pre><code>${escapeHtml(issue.fix_code || '')}</code></pre>
          </div>
        </div>
      `;
    });
  }
  elements.concurrencyWarningsList.innerHTML = concHtml;
}

// Update Markdown Preview in Export Center
async function updateMarkdownPreview(data) {
  try {
    const res = await fetch('/api/export/markdown', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    if (res.ok) {
      const json = await res.json();
      elements.markdownPreviewTextarea.value = json.markdown || '';
    }
  } catch (err) {
    console.warn('Failed to update markdown preview:', err);
  }
}

// Switch Main Tab
function switchTab(tabId) {
  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

  const targetTabBtn = document.querySelector(`.nav-tab[data-tab="${tabId}"]`);
  const targetContent = document.getElementById(`tab-${tabId}`);

  if (targetTabBtn) targetTabBtn.classList.add('active');
  if (targetContent) targetContent.classList.add('active');
}

// Switch Sub Tab
function switchSubTab(subTabId) {
  document.querySelectorAll('.sub-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.subtab-content').forEach(c => c.classList.remove('active'));

  const targetTabBtn = document.querySelector(`.sub-tab[data-subtab="${subTabId}"]`);
  const targetContent = document.getElementById(`subtab-${subTabId}`);

  if (targetTabBtn) targetTabBtn.classList.add('active');
  if (targetContent) targetContent.classList.add('active');
}

// Copy Single Issue
async function copySingleIssue(idx) {
  const issue = appState.filteredIssues[idx];
  if (!issue) return;

  const markdown = `### 🚨 [${issue.severity}] ${issue.title}
**التصنيف / Category:** ${issue.category_ar || issue.category}
**الملف المتضرر / File:** \`${issue.file}:${issue.line || 1}\`

#### 📋 وصف الخلل وتأثيره على التطبيق:
${issue.description_ar || ''}
> ${issue.description || ''}

#### 💻 مقتطف الكود الحالي:
\`\`\`kotlin
${issue.code_snippet || ''}
\`\`\`

#### 🛠️ الحل المقترح وكود التصحيح:
**التوجيه:** ${issue.fix_suggestion || ''}
\`\`\`kotlin
${issue.fix_code || ''}
\`\`\`
`;

  await copyToClipboard(markdown);
  showToast('📋 تم نسخ تفاصيل المشكلة وكود التصحيح بنجاح!');
}

// Copy Full Markdown Report
async function copyFullMarkdownReport() {
  const text = elements.markdownPreviewTextarea.value;
  if (!text) return;
  await copyToClipboard(text);
  showToast('📋 تم نسخ التقرير الكامل للحافظة!');
}

// Download Markdown Report
function downloadMarkdownReport() {
  const text = elements.markdownPreviewTextarea.value;
  if (!text) return;
  downloadFile(text, `RNDM_Audit_Report_${new Date().toISOString().slice(0, 10)}.md`, 'text/markdown');
  showToast('💾 تم تنزيل ملف التقرير Markdown!');
}

// Copy AI Prompt
async function copyAiPrompt() {
  if (!appState.scanData) return;
  const issues = appState.scanData.issues || [];
  
  let prompt = `# تعليمات إصلاح مشاكل الأداء وقواعد البيانات لتطبيق RNDM\n\n`;
  prompt += `يرجى مراجعة وتصحيح المشاكل المكتشفة التالية في تطبيق RNDM (Kotlin / Compose / Room) بالترتيب حسب الأولوية:\n\n`;

  issues.forEach((issue, i) => {
    prompt += `## ${i + 1}. [${issue.severity}] ${issue.title}\n`;
    prompt += `- الملف: \`${issue.file}:${issue.line || 1}\`\n`;
    prompt += `- الخلل: ${issue.description_ar}\n`;
    prompt += `- كود التصحيح المطلوب:\n\`\`\`kotlin\n${issue.fix_code}\n\`\`\`\n\n`;
  });

  await copyToClipboard(prompt);
  showToast('🤖 تم نسخ برومبت الذكاء الاصطناعي للإصلاح بنجاح!');
}

// Download JSON Report
function downloadJsonReport() {
  if (!appState.scanData) return;
  const jsonStr = JSON.stringify(appState.scanData, null, 2);
  downloadFile(jsonStr, `RNDM_Audit_Data_${new Date().toISOString().slice(0, 10)}.json`, 'application/json');
  showToast('💾 تم تنزيل ملف بيانات JSON!');
}

// Copy Filtered Markdown
async function copyFilteredMarkdownReport() {
  const issues = appState.filteredIssues;
  if (!issues || issues.length === 0) {
    showToast('⚠️ لا توجد مشاكل معروضة للنسخ');
    return;
  }

  let text = `# تقرير المشاكل المفلترة (${issues.length} مشكلة)\n\n`;
  issues.forEach((issue, i) => {
    text += `### ${i + 1}. [${issue.severity}] ${issue.title}\n`;
    text += `- الملف: \`${issue.file}:${issue.line}\`\n`;
    text += `- الوصف: ${issue.description_ar}\n`;
    text += `- الحل المقترح:\n\`\`\`kotlin\n${issue.fix_code}\n\`\`\`\n\n`;
  });

  await copyToClipboard(text);
  showToast(`📋 تم نسخ ${issues.length} مشكلة مفلترة بنجاح!`);
}

// Copy Entity Index Fix
async function copyEntityIndexFix(tblName) {
  const entity = appState.scanData?.database_details?.entities?.[tblName];
  if (!entity) return;

  const fks = entity.foreign_keys || [];
  const neededCols = fks.flatMap(fk => fk.child_columns);
  const idxCode = `@Entity(\n    tableName = "${tblName}",\n    indices = [\n` +
    neededCols.map(c => `        Index(value = ["${c}"])`).join(',\n') +
    `\n    ]\n)`;

  await copyToClipboard(idxCode);
  showToast(`📋 تم نسخ كود فهارس جدول ${tblName}!`);
}

// Copy All Database Fixes
async function copyAllDatabaseFixes() {
  const issues = (appState.scanData?.issues || []).filter(i => i.category === 'Database & Indexing');
  if (issues.length === 0) {
    showToast('✨ لا توجد مشاكل فهرسة مسجلة!');
    return;
  }

  let text = `// ==========================================\n// RNDM Database & Indexing Fixes\n// ==========================================\n\n`;
  issues.forEach(iss => {
    text += `// ${iss.title}\n// File: ${iss.file}:${iss.line}\n${iss.fix_code}\n\n`;
  });

  await copyToClipboard(text);
  showToast('📋 تم نسخ كافة أكواد إصلاح الفهارس وقواعد البيانات!');
}

// Helper: Copy to Clipboard
async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
  } catch (err) {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
  }
}

// Helper: Download File
function downloadFile(content, fileName, contentType) {
  const blob = new Blob([content], { type: contentType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// Helper: Show Toast
function showToast(message) {
  elements.toastMessage.textContent = message;
  elements.toastNotification.classList.add('show');
  setTimeout(() => {
    elements.toastNotification.classList.remove('show');
  }, 3500);
}

// Helper: Show/Hide Loading
function showLoading(show) {
  if (show) {
    elements.loadingOverlay.classList.add('active');
  } else {
    elements.loadingOverlay.classList.remove('active');
  }
}

// Helper: Escape HTML
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Theme Handling
function initTheme() {
  if (appState.theme === 'light') {
    document.body.classList.add('light-theme');
    elements.btnThemeToggle.innerHTML = '<i class="fa-solid fa-sun"></i>';
  } else {
    document.body.classList.remove('light-theme');
    elements.btnThemeToggle.innerHTML = '<i class="fa-solid fa-moon"></i>';
  }
}

function toggleTheme() {
  if (document.body.classList.contains('light-theme')) {
    document.body.classList.remove('light-theme');
    appState.theme = 'dark';
    elements.btnThemeToggle.innerHTML = '<i class="fa-solid fa-moon"></i>';
  } else {
    document.body.classList.add('light-theme');
    appState.theme = 'light';
    elements.btnThemeToggle.innerHTML = '<i class="fa-solid fa-sun"></i>';
  }
  localStorage.setItem('rndm_theme', appState.theme);
}
