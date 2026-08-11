// State Store
const state = {
    token: localStorage.getItem('jwt_token') || null,
    user: JSON.parse(localStorage.getItem('user_profile') || 'null'),
    currentRoute: '',
    adminTab: 'teachers', // 'teachers' | 'tasks' | 'monitoring' | 'dictionaries'
    teacherTab: 'variants', // 'variants' | 'create-variant' | 'students' | 'profile'
    adminTasks: [],
    adminTeachers: [],
    teacherVariants: [],
    teacherStudents: [],
    selectedTaskIds: [],
    studentTest: null,
    studentAttempt: null,
    studentResult: null,
    studentAnswers: {},
    teacherTempPasswords: {},
    taskFilters: { subject: '', exam: '', bank: '', subtopic: '', search: '' },
    dictionaries: {
        subjects: [],
        exams: [],
        banks: []
    }
};

// Non-blocking Toast Notification System (replaces showToast())
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position:fixed;top:1.5rem;right:1.5rem;z-index:9999;display:flex;flex-direction:column;gap:0.5rem;max-width:420px;';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast-notification toast-' + type;
    toast.style.cssText = 'padding:1rem 1.5rem;border-radius:0.75rem;font-size:0.9rem;line-height:1.4;color:#fff;box-shadow:0 4px 20px rgba(0,0,0,0.2);animation:toastIn 0.3s ease;cursor:pointer;white-space:pre-line;';
    const bg = type === 'error' ? '#ef4444' : type === 'success' ? '#22c55e' : '#3b82f6';
    toast.style.background = bg;
    toast.textContent = message;
    toast.onclick = () => toast.remove();
    container.appendChild(toast);
    setTimeout(() => { if (toast.parentNode) toast.remove(); }, 5000);
}


// Clipboard copy helper with interactive button feedback
function copyToClipboard(text, btnElement = null) {
    try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(() => {
                showToast('Ссылка скопирована в буфер обмена!', 'success');
                if (btnElement) {
                    const originalText = btnElement.innerHTML;
                    btnElement.innerHTML = 'Скопировано!';
                    btnElement.classList.add('btn-copy-success');
                    setTimeout(() => {
                        btnElement.innerHTML = originalText;
                        btnElement.classList.remove('btn-copy-success');
                    }, 2000);
                }
            }).catch(() => {
                showToast('Не удалось скопировать ссылку', 'error');
            });
        }
    } catch (e) {
        showToast('Ошибка доступа к буферу обмена', 'error');
    }
}


// Helper: Digital Browser Fingerprint Generator
function getBrowserFingerprint() {
    try {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        ctx.textBaseline = "top";
        ctx.font = "14px 'Arial'";
        ctx.fillStyle = "#f60";
        ctx.fillRect(125, 1, 62, 20);
        ctx.fillStyle = "#069";
        ctx.fillText("Reshaemo,fp:v1", 2, 15);
        const dataUrl = canvas.toDataURL();
        let hash = 0;
        for (let i = 0; i < dataUrl.length; i++) {
            const char = dataUrl.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash |= 0;
        }
        return 'fp-' + Math.abs(hash).toString(16) + '-' + screen.width + 'x' + screen.height;
    } catch (e) {
        return 'fp-standard-' + screen.width + 'x' + screen.height;
    }
}

// Global Input Mask Applier
function applyInputMasks() {
    // Latin-only fields (logins)
    document.querySelectorAll('input[data-mask="latin"], input[id*="login"]').forEach(input => {
        input.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/[^a-zA-Z0-9_.-]/g, '');
        });
    });

    // Password fields (Latin/ASCII only, no Cyrillic)
    document.querySelectorAll('input[type="password"], input[data-mask="password"]').forEach(input => {
        input.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/[\u0400-\u04FF]/g, '');
        });
    });

    // Cyrillic-only fields (student name)
    document.querySelectorAll('input[data-mask="cyrillic"], input[id*="student-name"]').forEach(input => {
        input.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/[^а-яА-ЯёЁ\s-]/g, '');
        });
    });
}

// KaTeX LaTeX renderer helper
function triggerKaTeX() {
    if (window.renderMathInElement) {
        try {
            window.renderMathInElement(document.body, {
                delimiters: [
                    {left: '$$', right: '$$', display: true},
                    {left: '$', right: '$', display: false},
                    {left: '\\(', right: '\\)', display: false},
                    {left: '\\[', right: '\\]', display: true}
                ],
                throwOnError: false
            });
        } catch (e) {
            console.warn('KaTeX render error:', e);
        }
    }
}

function updateTaskContentPreview(val) {
    const preview = document.getElementById('live-task-preview');
    if (preview) {
        let cleaned = (val || '')
            .replace(/[\u2061\u2062\u2063\u2064\u200b\u200c\u200d\u200e\u200f\ufeff]/g, '')
            .replace(/\u2009/g, ' ')
            .replace(/\u200a/g, ' ')
            .replace(/\u202f/g, ' ')
            .replace(/\u00a0/g, ' ')
            .replace(/−/g, '-');
        preview.innerHTML = cleaned || '<span style="color:var(--text-secondary); font-style:italic;">Введите текст задачи или формулы LaTeX ($...$)...</span>';
        triggerKaTeX();
    }
}

function showButtonFeedback(btn) {
    if (!btn || btn.dataset.feedbacking === 'true') return;
    btn.dataset.feedbacking = 'true';
    const oldHtml = btn.innerHTML;
    const oldTransform = btn.style.transform;

    btn.style.transform = 'scale(0.96)';
    btn.style.transition = 'all 0.12s ease';

    if (!oldHtml.includes('✓') && !oldHtml.includes('Готово')) {
        btn.innerHTML = '✓ ' + oldHtml;
    }

    setTimeout(() => {
        btn.innerHTML = oldHtml;
        btn.style.transform = oldTransform;
        delete btn.dataset.feedbacking;
    }, 1200);
}

function insertLatexTag(textareaId, rawStartTag, rawEndTag, btnEvt) {
    const textarea = document.getElementById(textareaId);
    if (!textarea) return;

    if (btnEvt) {
        showButtonFeedback(btnEvt.currentTarget || btnEvt.target);
    }

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const val = textarea.value;
    const selectedText = val.substring(start, end);

    // Automatically wrap math expressions in double dollar signs $$...$$
    const startTag = rawStartTag.startsWith('$$') ? rawStartTag : '$$' + rawStartTag;
    const endTag = rawEndTag.endsWith('$$') ? rawEndTag : rawEndTag + '$$';

    let replacement = '';
    let newCursorPos = start + startTag.length;

    if (selectedText.length > 0) {
        replacement = startTag + selectedText + endTag;
        newCursorPos = start + replacement.length - endTag.length;
    } else {
        replacement = startTag + endTag;
        newCursorPos = start + startTag.length;
    }

    textarea.value = val.substring(0, start) + replacement + val.substring(end);
    textarea.focus();
    textarea.setSelectionRange(newCursorPos, newCursorPos);
    updateTaskContentPreview(textarea.value);
}

function attachPasteSanitizer(textareaId) {
    const textarea = document.getElementById(textareaId);
    if (!textarea) return;

    textarea.addEventListener('paste', (e) => {
        e.preventDefault();
        const pastedText = (e.clipboardData || window.clipboardData).getData('text');
        if (!pastedText) return;

        let sanitized = pastedText
            .replace(/[\u2061\u2062\u2063\u2064\u200b\u200c\u200d\u200e\u200f\ufeff]/g, '')
            .replace(/\u2009/g, ' ')
            .replace(/\u200a/g, ' ')
            .replace(/\u202f/g, ' ')
            .replace(/\u00a0/g, ' ')
            .replace(/−/g, '-')
            .replace(/[\r\n]+/g, ' ')
            .replace(/\s+/g, ' ');

        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const val = textarea.value;

        textarea.value = val.substring(0, start) + sanitized + val.substring(end);
        const newCursorPos = start + sanitized.length;
        textarea.setSelectionRange(newCursorPos, newCursorPos);
        updateTaskContentPreview(textarea.value);
    });
}

// Main App Controller
function initApp() {
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('button, .btn');
        if (btn && !btn.disabled && !btn.classList.contains('no-feedback')) {
            showButtonFeedback(btn);
        }
    });

    window.addEventListener('hashchange', handleRoute);
    if (!window.location.hash) {
        window.location.hash = '#login';
    } else {
        handleRoute();
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initApp);
} else {
    initApp();
}


// Router
function handleRoute() {
    const hash = window.location.hash || '#login';
    state.currentRoute = hash;

    if (hash.startsWith('#test/')) {
        const accessToken = hash.replace('#test/', '');
        renderStudentTestView(accessToken);
        return;
    }

    if (hash === '#login') {
        renderLoginView();
        return;
    }

    if (!state.token) {
        window.location.hash = '#login';
        return;
    }

    if (hash === '#admin' && state.user && state.user.role === 'ADMIN') {
        renderAdminDashboard();
        return;
    }

    if (hash === '#teacher') {
        renderTeacherDashboard();
        return;
    }

    // Default fallback
    if (state.user && state.user.role === 'ADMIN') {
        window.location.hash = '#admin';
    } else {
        window.location.hash = '#teacher';
    }
}

// API Helper
async function apiFetch(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json;charset=UTF-8',
        ...(options.headers || {})
    };

    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }

    const response = await fetch(endpoint, { ...options, headers });

    if (response.status === 401 || (response.status === 403 && endpoint.startsWith('/api/admin'))) {
        if (state.token) {
            logout();
            throw new Error('Сессия истекла или недостаточно прав. Пожалуйста, войдите заново');
        }
    }

    if (response.status === 204) {
        return {};
    }

    const text = await response.text();

    if (!response.ok) {
        let errData = {};
        try { errData = JSON.parse(text); } catch (e) { errData = { error: text || 'Ошибка сервера' }; }
        throw new Error(errData.error || errData.message || 'Ошибка сервера');
    }

    if (!text || text.trim().length === 0) {
        return {};
    }

    try {
        return JSON.parse(text);
    } catch (e) {
        return { success: true };
    }
}

function logout() {
    state.token = null;
    state.user = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_profile');
    window.location.hash = '#login';
}

// ==========================================
// LOGIN VIEW
// ==========================================
function renderLoginView() {
    const app = document.getElementById('app');
    app.innerHTML = `
        <div style="min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 1.5rem;">
            <div class="card" style="width: 100%; max-width: 420px; box-shadow: var(--shadow-hover);">
                <div style="text-align: center; margin-bottom: 2rem;">
                    <h1 style="font-size: 2.2rem; font-weight: 800; color: var(--primary); letter-spacing: -0.03em;">Reshaemo</h1>
                    <p style="color: var(--text-secondary); font-size: 0.9rem; margin-top: 0.3rem;">Вход для Администраторов и Преподавателей</p>
                </div>

                <div id="login-error" class="badge badge-danger" style="display: none; margin-bottom: 1rem; width: 100%; padding: 0.8rem;"></div>

                <form id="login-form">
                    <div class="form-group">
                        <label class="form-label">Логин (только латиница)</label>
                        <input type="text" id="login-input" data-mask="latin" class="form-control" placeholder="admin" required>
                        <small class="input-hint">Строго на английском языке (буквы A-Z, a-z, 0-9, _ . -)</small>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Пароль</label>
                        <input type="password" id="password-input" data-mask="password" class="form-control" placeholder="••••••••" required>
                        <small class="input-hint">Без кириллицы (только латинские буквы и символы)</small>
                    </div>

                    <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem; padding: 0.8rem; font-size: 1rem;">
                        Войти в систему
                    </button>
                </form>
            </div>
        </div>
    `;

    applyInputMasks();

    document.getElementById('login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const login = document.getElementById('login-input').value.trim();
        const password = document.getElementById('password-input').value;

        const errorDiv = document.getElementById('login-error');
        errorDiv.style.display = 'none';

        try {
            const data = await apiFetch('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ login, password })
            });

            state.token = data.token;
            const userObj = data.user || data;
            state.user = {
                id: userObj.id || userObj.userId,
                login: userObj.login,
                displayName: userObj.displayName,
                role: userObj.role,
                temporaryPassword: userObj.temporaryPassword
            };

            localStorage.setItem('jwt_token', data.token);
            localStorage.setItem('user_profile', JSON.stringify(state.user));

            if (state.user.role === 'ADMIN') {
                window.location.hash = '#admin';
            } else {
                window.location.hash = '#teacher';
            }

        } catch (err) {
            errorDiv.textContent = err.message;
            errorDiv.style.display = 'block';
        }
    });
}

// ==========================================
// ADMIN DASHBOARD VIEW
// ==========================================
async function renderAdminDashboard() {
    const app = document.getElementById('app');
    app.innerHTML = `
        <nav class="navbar">
            <a href="#admin" class="navbar-brand">Reshaemo <span class="logo-tag">Admin</span></a>
            <div class="navbar-user">
                <span class="user-badge">${state.user.displayName} (Администратор)</span>
                <button onclick="logout()" class="btn btn-secondary btn-sm">Выйти</button>
            </div>
        </nav>

        <div class="container">
            <div class="tabs">
                <button class="tab-btn ${state.adminTab === 'teachers' ? 'active' : ''}" onclick="switchAdminTab('teachers', event)">Преподаватели</button>
                <button class="tab-btn ${state.adminTab === 'tasks' ? 'active' : ''}" onclick="switchAdminTab('tasks', event)">Банк Задач</button>
                <button class="tab-btn ${state.adminTab === 'dictionaries' ? 'active' : ''}" onclick="switchAdminTab('dictionaries', event)">Справочники</button>
                <button class="tab-btn ${state.adminTab === 'monitoring' ? 'active' : ''}" onclick="switchAdminTab('monitoring', event)">Мониторинг</button>
            </div>

            <div id="admin-tab-content"></div>
        </div>
    `;

    loadAdminTabContent();
}

function switchAdminTab(tab, evt) {
    state.adminTab = tab;
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    if (evt && evt.target) {
        evt.target.classList.add('active');
    }
    loadAdminTabContent();
}


async function loadAdminTabContent(forceRefresh = false) {
    const container = document.getElementById('admin-tab-content');
    if (state.adminTab === 'teachers') {
        await loadAdminTeachersTab(container, forceRefresh);
    } else if (state.adminTab === 'tasks') {
        await loadAdminTasksTab(container, forceRefresh);
    } else if (state.adminTab === 'dictionaries') {
        await loadAdminDictionariesTab(container, forceRefresh);
    } else if (state.adminTab === 'monitoring') {
        await loadAdminMonitoringTab(container, forceRefresh);
    }
}

async function loadAdminTeachersTab(container, forceRefresh = false) {
    if (!state.adminTeachers || forceRefresh || !container.children.length) {
        if (!container.children.length) {
            container.innerHTML = '<p style="text-align:center; padding: 2rem; color: var(--text-secondary);">Загрузка списка преподавателей...</p>';
        }
        try {
            const teachers = await apiFetch('/api/admin/users');
            state.adminTeachers = teachers;
        } catch (err) {
            container.innerHTML = `<div class="badge badge-danger">Ошибка загрузки: ${err.message}</div>`;
            return;
        }
    }
    const teachers = state.adminTeachers || [];

    container.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
            <h2 style="font-size: 1.4rem; font-weight: 700;">Управление Преподавателями (${teachers.length})</h2>
            <button onclick="renderCreateTeacherModal()" class="btn btn-primary">+ Добавить преподавателя</button>
        </div>

        <div class="card" style="padding: 0; overflow: hidden;">
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background: var(--bg-hover); text-align: left;">
                        <th style="padding: 1rem;">ID</th>
                        <th style="padding: 1rem;">ФИО Преподавателя</th>
                        <th style="padding: 1rem;">Логин</th>
                        <th style="padding: 1rem;">Пароль / Статус</th>
                        <th style="padding: 1rem;">Тестов</th>
                        <th style="padding: 1rem;">Активна подписка до</th>
                        <th style="padding: 1rem;">Действия</th>
                    </tr>
                </thead>
                <tbody>
                    ${teachers.map(t => {
                        const nextSubDate = t.nextPaymentAt ? new Date(t.nextPaymentAt).toLocaleDateString('ru-RU') : 'Не ограничена';
                        return `
                        <tr id="teacher-row-${t.id}" style="border-top: 1px solid var(--border-color);">
                            <td style="padding: 1rem;">#${t.id}</td>
                            <td style="padding: 1rem;"><strong>${t.displayName}</strong></td>
                            <td style="padding: 1rem;"><code>${t.login}</code></td>
                            <td style="padding: 1rem;">
                                ${t.temporaryPassword ? `
                                    <span class="badge badge-warning">Временный</span>
                                    <button id="btn-copy-pass-${t.id}" data-pass="${(state.teacherTempPasswords && state.teacherTempPasswords[t.id]) || t.temporaryPasswordStr || ''}" onclick="copyTeacherPassword(${t.id}, event)" class="btn btn-sm btn-secondary" style="margin-left:5px;">📋 Пароль</button>
                                ` : '<span class="badge badge-success">Постоянный</span>'}
                            </td>
                            <td style="padding: 1rem;">${t.testsCreatedCount || t.createdVariantsCount || 0}</td>
                            <td style="padding: 1rem;">
                                <span class="badge badge-info">${nextSubDate}</span>
                                <button onclick="renderUpdateSubscriptionModal(${t.id})" class="btn btn-sm btn-secondary" style="margin-left:5px;" title="Изменить дату подписки">📅</button>
                            </td>
                            <td style="padding: 1rem;">
                                <button onclick="renderEditTeacherModal(${t.id})" class="btn btn-secondary btn-sm" title="Редактировать ФИО и Логин">✏️</button>
                                <button onclick="copyToClipboard('${t.login}')" class="btn btn-sm btn-secondary" title="Скопировать логин">📋 Логин</button>
                                <button onclick="resetTeacherPassword(${t.id})" class="btn btn-secondary btn-sm">Сбросить пароль</button>
                                <button onclick="deleteTeacher(${t.id})" class="btn btn-danger btn-sm">Удалить</button>
                            </td>
                        </tr>
                    `}).join('') || '<tr><td colspan="7" style="padding: 2rem; text-align: center;">Преподаватели отсутствуют</td></tr>'}
                </tbody>
            </table>
        </div>
    `;
}

function renderEditTeacherModal(id) {
    const teacher = (state.adminTeachers || []).find(t => t.id === id);
    if (!teacher) return;

    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div class="card" style="width: 100%; max-width: 450px;">
                <h2 style="font-size: 1.3rem; margin-bottom: 1rem;">Редактирование учетной записи</h2>
                <form id="edit-teacher-form">
                    <div class="form-group">
                        <label class="form-label">ФИО Преподавателя</label>
                        <input type="text" id="edit-teacher-name" class="form-control" value="${teacher.displayName || ''}" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Логин</label>
                        <input type="text" id="edit-teacher-login" class="form-control" value="${teacher.login || ''}" required>
                    </div>
                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Сохранить</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    document.getElementById('edit-teacher-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const displayName = document.getElementById('edit-teacher-name').value.trim();
        const login = document.getElementById('edit-teacher-login').value.trim();

        try {
            await apiFetch(`/api/admin/users/${id}`, {
                method: 'PUT',
                body: JSON.stringify({ displayName, login })
            });
            closeModal();
            showToast('Данные преподавателя успешно обновлены!');
            loadAdminTabContent();
        } catch (err) {
            showToast('Ошибка при обновлении: ' + err.message, 'error');
        }
    });
}

function copyTeacherPassword(id, evt) {
    const btn = evt ? (evt.currentTarget || evt.target) : document.getElementById(`btn-copy-pass-${id}`);
    let pass = (state.teacherTempPasswords && state.teacherTempPasswords[id]) ? state.teacherTempPasswords[id] : (btn ? btn.getAttribute('data-pass') : '');

    if (!pass || pass.trim().length === 0) {
        showToast('Временный пароль доступен сразу после создания или сброса пароля.', 'info');
        return;
    }

    copyToClipboard(pass);
    if (btn) {
        const oldHtml = btn.innerHTML;
        btn.innerHTML = '✓ Скопировано!';
        btn.style.background = '#22c55e';
        btn.style.borderColor = '#22c55e';
        btn.style.color = '#ffffff';
        setTimeout(() => {
            btn.innerHTML = oldHtml;
            btn.style.background = '';
            btn.style.borderColor = '';
            btn.style.color = '';
        }, 2000);
    }
    showToast('Пароль скопирован в буфер обмена!');
}

function renderCreateTeacherModal() {
    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div class="card" style="width: 100%; max-width: 450px;">
                <h2 style="font-size: 1.3rem; margin-bottom: 1rem;">Регистрация Преподавателя</h2>
                
                <form id="create-teacher-form">
                    <div class="form-group">
                        <label class="form-label">ФИО Преподавателя</label>
                        <input type="text" id="teacher-name" class="form-control" placeholder="Иванова Анна Сергеевна" required>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Логин (только латиница)</label>
                        <input type="text" id="teacher-login" data-mask="latin" class="form-control" placeholder="teacher_ivanova" required>
                        <small class="input-hint">Строго буквы A-Z, a-z, 0-9, _ . -</small>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Дата окончания подписки (необязательно)</label>
                        <input type="date" id="teacher-sub-date" class="form-control">
                    </div>

                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Создать аккаунт</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    applyInputMasks();

    document.getElementById('create-teacher-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const displayName = document.getElementById('teacher-name').value.trim();
        const login = document.getElementById('teacher-login').value.trim();
        const subDate = document.getElementById('teacher-sub-date').value;
        const nextPaymentAt = subDate ? new Date(subDate).toISOString() : null;

        try {
            const res = await apiFetch('/api/admin/users', {
                method: 'POST',
                body: JSON.stringify({ displayName, login, nextPaymentAt })
            });

            if (res.user && res.user.id && res.temporaryPassword) {
                state.teacherTempPasswords[res.user.id] = res.temporaryPassword;
            }

            closeModal();
            showToast(`Преподаватель создан!\nЛогин: ${res.user.login}\nВременный пароль: ${res.temporaryPassword}`);
            copyToClipboard(res.temporaryPassword);
            loadAdminTabContent();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });
}

function renderUpdateSubscriptionModal(id) {
    const teacher = state.adminTeachers.find(t => t.id === id);
    const currentDate = (teacher && teacher.nextPaymentAt) ? teacher.nextPaymentAt.split('T')[0] : '';

    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div class="card" style="width: 100%; max-width: 420px;">
                <h2 style="font-size: 1.3rem; margin-bottom: 1rem;">Управление подпиской</h2>
                <p style="color: var(--text-secondary); margin-bottom: 1rem;">Преподаватель: <strong>${teacher ? teacher.displayName : ''}</strong></p>
                
                <form id="update-sub-form">
                    <div class="form-group">
                        <label class="form-label">Дата окончания активной подписки</label>
                        <input type="date" id="sub-next-date" class="form-control" value="${currentDate}" required>
                    </div>

                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Сохранить подписку</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    document.getElementById('update-sub-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const dateVal = document.getElementById('sub-next-date').value;
        const nextPaymentAt = dateVal ? new Date(dateVal).toISOString() : null;

        try {
            await apiFetch(`/api/admin/users/${id}/subscription`, {
                method: 'PUT',
                body: JSON.stringify({ nextPaymentAt })
            });
            closeModal();
            showToast('Дата подписки успешно обновлена!');
            loadAdminTabContent();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });
}

async function resetTeacherPassword(id) {
    if (!confirm('Сбросить пароль преподавателя?')) return;
    try {
        const res = await apiFetch(`/api/admin/users/${id}/reset-password`, { method: 'POST' });
        if (res.user && res.user.id && res.temporaryPassword) {
            state.teacherTempPasswords[res.user.id] = res.temporaryPassword;
        }
        showToast(`Пароль сброшен!\nНовый временный пароль: ${res.temporaryPassword}`);
        copyToClipboard(res.temporaryPassword);
        loadAdminTabContent();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteTeacher(id) {
    if (!confirm('Удалить преподавателя и все его данные?')) return;
    try {
        const tr = document.getElementById(`teacher-row-${id}`);
        if (tr) tr.remove();
        state.adminTeachers = state.adminTeachers.filter(t => t.id !== id);
        await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
        showToast('Преподаватель успешно удален');
    } catch (err) {
        showToast(err.message, 'error');
        loadAdminTabContent();
    }
}

async function uploadZipArchive(inputElement) {
    if (!inputElement.files || !inputElement.files[0]) return;
    const file = inputElement.files[0];
    const formData = new FormData();
    formData.append('file', file);

    // Отрисовка модального окна с процентами прогресса импорта
    const progressModalHtml = `
        <div id="zip-progress-modal" style="position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 9999; padding: 1.5rem;">
            <div class="card" style="width: 100%; max-width: 500px; text-align: center; padding: 2rem;">
                <div style="font-size: 2.5rem; margin-bottom: 0.5rem;" id="zip-icon">📦</div>
                <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 0.5rem;" id="zip-modal-title">Импорт архива задач</h3>
                <p id="zip-progress-status" style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 1.5rem;">
                    Подготовка файла ${file.name} (${(file.size / (1024 * 1024)).toFixed(1)} МБ)...
                </p>

                <div style="width: 100%; height: 16px; background: var(--bg-hover); border-radius: 8px; overflow: hidden; margin-bottom: 0.8rem; position: relative;">
                    <div id="zip-progress-bar" style="width: 0%; height: 100%; background: linear-gradient(90deg, #3b82f6, #10b981); border-radius: 8px; transition: width 0.2s ease;"></div>
                </div>

                <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: 600; color: var(--text-primary);">
                    <span id="zip-progress-percent">0%</span>
                    <span id="zip-progress-bytes">0 / ${(file.size / (1024 * 1024)).toFixed(1)} МБ</span>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', progressModalHtml);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/admin/tasks/import-zip', true);
    if (state.token) {
        xhr.setRequestHeader('Authorization', `Bearer ${state.token}`);
    }

    const progressBar = document.getElementById('zip-progress-bar');
    const progressPercent = document.getElementById('zip-progress-percent');
    const progressBytes = document.getElementById('zip-progress-bytes');
    const progressStatus = document.getElementById('zip-progress-status');
    const modalTitle = document.getElementById('zip-modal-title');
    const zipIcon = document.getElementById('zip-icon');

    xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
            const pct = Math.round((e.loaded / e.total) * 100);
            if (pct < 100) {
                if (progressBar) progressBar.style.width = pct + '%';
                if (progressPercent) progressPercent.innerText = pct + '%';
                if (progressBytes) progressBytes.innerText = `${(e.loaded / (1024 * 1024)).toFixed(1)} / ${(e.total / (1024 * 1024)).toFixed(1)} МБ`;
                if (progressStatus) progressStatus.innerText = `Передача архива ${file.name} на сервер (${pct}%)...`;
            } else {
                if (progressBar) {
                    progressBar.style.width = '100%';
                    progressBar.style.background = 'linear-gradient(90deg, #10b981, #6366f1)';
                }
                if (progressPercent) progressPercent.innerText = '100%';
                if (progressBytes) progressBytes.innerText = 'Пакетная запись в СУБД...';
                if (modalTitle) modalTitle.innerText = 'Обработка задач в PostgreSQL';
                if (zipIcon) zipIcon.innerText = '⚙️';
                if (progressStatus) {
                    progressStatus.innerHTML = `
                        <strong style="color: var(--success);">Файл передан на сервер!</strong><br>
                        Выполняется распаковка ZIP, парсинг Excel и запись задач в PostgreSQL.<br>
                        <small style="color: var(--text-secondary);">Пожалуйста, подождите (не закрывайте окно)...</small>
                    `;
                }
            }
        }
    };

    xhr.onload = () => {
        const modal = document.getElementById('zip-progress-modal');
        if (modal) modal.remove();

        // Сбрасываем локальный кэш задач для загрузки свежих из БД
        state.adminTasks = null;

        if (xhr.status >= 200 && xhr.status < 300) {
            try {
                const res = JSON.parse(xhr.responseText);
                let msg = `Импорт успешно завершен!\nВсего обработано: ${res.totalProcessed}\nСоздано: ${res.createdCount}\nОбновлено: ${res.updatedCount}\nКартинок привязано: ${res.imagesAttachedCount}`;
                if (res.warnings && res.warnings.length) {
                    msg += `\nПредупреждений: ${res.warnings.length}`;
                }
                showToast(msg, 'success');
                loadAdminTabContent(true);
            } catch (e) {
                showToast('Архив загружен, обновляем список задач...', 'info');
                loadAdminTabContent(true);
            }
        } else {
            let errMessage = 'Ошибка импорта ZIP архива';
            try {
                const res = JSON.parse(xhr.responseText);
                if (res.message) errMessage = res.message;
            } catch (e) {}
            showToast(errMessage, 'error');
        }
    };

    xhr.onerror = () => {
        const modal = document.getElementById('zip-progress-modal');
        if (modal) modal.remove();
        showToast('Сетевая ошибка при загрузке архива', 'error');
    };

    xhr.send(formData);
}

// ==========================================
// ADMIN TASKS BANK
// ==========================================
async function loadAdminTasksTab(container, forceRefresh = false) {
    if (!state.adminTasks || forceRefresh) {
        if (!container.children.length) {
            container.innerHTML = '<p style="text-align:center; padding: 2rem; color: var(--text-secondary);">Загрузка банка задач...</p>';
        }
        try {
            const [tasks, subjects, exams, banks] = await Promise.all([
                apiFetch('/api/admin/tasks'),
                apiFetch('/api/admin/dictionaries/subjects').catch(() => []),
                apiFetch('/api/admin/dictionaries/exams').catch(() => []),
                apiFetch('/api/admin/dictionaries/banks').catch(() => [])
            ]);

            state.adminTasks = tasks;
            if (subjects.length) state.dictionaries.subjects = subjects;
            if (exams.length) state.dictionaries.exams = exams;
            if (banks.length) state.dictionaries.banks = banks;
        } catch (err) {
            container.innerHTML = `<div class="badge badge-danger">Ошибка загрузки: ${err.message}</div>`;
            return;
        }
    }

    const tasks = state.adminTasks || [];
    const f = state.taskFilters;
    if (!f.pageSize) f.pageSize = 20;
    if (!f.currentPage) f.currentPage = 1;

    // Фильтрация
    const filteredTasks = tasks.filter(t => {
        if (f.search && !t.publicId.toLowerCase().includes(f.search.toLowerCase())) return false;
        if (f.subject && t.subject !== f.subject) return false;
        if (f.exam && t.examType !== f.exam) return false;
        if (f.bank && t.taskBank !== f.bank) return false;
        if (f.subtopic && (!t.subtopic || !t.subtopic.toLowerCase().includes(f.subtopic.toLowerCase()))) return false;
        if (f.taskNumber && String(t.taskNumber || '').trim() !== String(f.taskNumber).trim()) return false;
        return true;
    });

    // Пагинация (ограничение до 50 задач на страницу max)
    const pageSize = Math.min(parseInt(f.pageSize) || 20, 50);
    const totalPages = Math.ceil(filteredTasks.length / pageSize) || 1;
    if (f.currentPage > totalPages) f.currentPage = totalPages;
    if (f.currentPage < 1) f.currentPage = 1;

    const startIndex = (f.currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, filteredTasks.length);
    const paginatedTasks = filteredTasks.slice(startIndex, endIndex);

    container.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
            <h2 style="font-size: 1.4rem; font-weight: 700;">Банк Задач (${filteredTasks.length} из ${tasks.length})</h2>
            <div style="display: flex; gap: 0.5rem;">
                <input type="file" id="zip-file-input" accept=".zip" style="display: none;" onchange="uploadZipArchive(this)">
                <button onclick="document.getElementById('zip-file-input').click()" class="btn btn-secondary">📦 Импорт из ZIP</button>
                <button onclick="renderCreateTaskModal()" class="btn btn-primary">+ Создать задачу</button>
            </div>
        </div>

        <!-- Filters Bar -->
        <div class="card" style="margin-bottom: 1rem; padding: 1rem;">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 0.8rem; align-items: end;">
                <div>
                    <label class="form-label" style="font-size:0.8rem;">Public ID</label>
                    <input type="text" id="task-filter-search" class="form-control" placeholder="T-1001..." value="${f.search || ''}" oninput="updateTaskFilterState('search', this.value)">
                </div>
                <div>
                    <label class="form-label" style="font-size:0.8rem;">№ Задания КИМ</label>
                    <input type="number" id="task-filter-number" class="form-control" placeholder="1, 2, 3..." value="${f.taskNumber || ''}" oninput="updateTaskFilterState('taskNumber', this.value)">
                </div>
                <div>
                    <label class="form-label" style="font-size:0.8rem;">Предмет</label>
                    <select id="task-filter-subject" class="form-control" onchange="updateTaskFilterState('subject', this.value)">
                        <option value="">Все предметы</option>
                        ${state.dictionaries.subjects.map(s => `<option value="${s.name}" ${f.subject === s.name ? 'selected' : ''}>${s.displayName}</option>`).join('')}
                    </select>
                </div>
                <div>
                    <label class="form-label" style="font-size:0.8rem;">Экзамен</label>
                    <select id="task-filter-exam" class="form-control" onchange="updateTaskFilterState('exam', this.value)">
                        <option value="">Все экзамены</option>
                        ${state.dictionaries.exams.map(e => `<option value="${e.name}" ${f.exam === e.name ? 'selected' : ''}>${e.displayName}</option>`).join('')}
                    </select>
                </div>
                <div>
                    <label class="form-label" style="font-size:0.8rem;">Банк</label>
                    <select id="task-filter-bank" class="form-control" onchange="updateTaskFilterState('bank', this.value)">
                        <option value="">Все банки</option>
                        ${state.dictionaries.banks.map(b => `<option value="${b.name}" ${f.bank === b.name ? 'selected' : ''}>${b.displayName}</option>`).join('')}
                    </select>
                </div>
                <div>
                    <label class="form-label" style="font-size:0.8rem;">Подтема</label>
                    <input type="text" id="task-filter-subtopic" class="form-control" placeholder="Подтема..." value="${f.subtopic || ''}" oninput="updateTaskFilterState('subtopic', this.value)">
                </div>
                <div>
                    <label class="form-label" style="font-size:0.8rem; font-weight: 700; color: var(--accent-color);">Показывать по</label>
                    <select id="task-filter-size" class="form-control" style="font-weight: 600;" onchange="updateTaskFilterState('pageSize', parseInt(this.value))">
                        <option value="10" ${pageSize === 10 ? 'selected' : ''}>10 задач</option>
                        <option value="20" ${pageSize === 20 ? 'selected' : ''}>20 задач</option>
                        <option value="30" ${pageSize === 30 ? 'selected' : ''}>30 задач</option>
                        <option value="40" ${pageSize === 40 ? 'selected' : ''}>40 задач</option>
                        <option value="50" ${pageSize === 50 ? 'selected' : ''}>50 задач</option>
                    </select>
                </div>
            </div>
        </div>

        <div class="card" style="padding: 0; overflow: hidden; margin-bottom: 1rem;">
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background: var(--bg-hover); text-align: left;">
                        <th style="padding: 1rem;">Public ID</th>
                        <th style="padding: 1rem;">Экзамен / Предмет</th>
                        <th style="padding: 1rem;">№ КИМ</th>
                        <th style="padding: 1rem;">Подтема</th>
                        <th style="padding: 1rem;">Банк</th>
                        <th style="padding: 1rem;">Ответ</th>
                        <th style="padding: 1rem;">Действия</th>
                    </tr>
                </thead>
                <tbody>
                    ${paginatedTasks.map(t => `
                        <tr style="border-top: 1px solid var(--border-color);">
                            <td style="padding: 1rem;"><code>${t.publicId}</code></td>
                            <td style="padding: 1rem;"><strong>${t.examType}</strong> / ${t.subject}</td>
                            <td style="padding: 1rem;"><span class="badge badge-warning">Задание №${t.taskNumber || '—'}</span></td>
                            <td style="padding: 1rem;">${t.subtopic || '—'}</td>
                            <td style="padding: 1rem;"><span class="badge badge-info">${t.taskBank}</span></td>
                            <td style="padding: 1rem;"><code>${t.correctAnswer || '—'}</code></td>
                            <td style="padding: 1rem;">
                                <button id="btn-expand-task-${t.id}" onclick="toggleTaskInspector(${t.id})" class="btn btn-secondary btn-sm" style="margin-right:4px;">👁️ Развернуть</button>
                                <button onclick="renderEditTaskModal(${t.id})" class="btn btn-secondary btn-sm">✏️ Изменить</button>
                                <button onclick="deleteTask(${t.id})" class="btn btn-danger btn-sm" style="margin-left:4px;">Удалить</button>
                            </td>
                        </tr>
                        <tr id="task-detail-row-${t.id}" style="display: none; background: var(--bg-hover);">
                            <td colspan="7" style="padding: 1.2rem;">
                                <div style="font-weight: 600; margin-bottom: 0.5rem; color: var(--text-secondary);">Полный текст задачи (${t.publicId}):</div>
                                <div class="task-content" style="padding: 1rem; background: var(--bg-card); border-radius: 0.5rem; border: 1px solid var(--border-color); margin-bottom: 1rem;">
                                    ${t.content || ''}
                                </div>
                                ${t.images && t.images.length ? `
                                    <div class="task-image-grid" style="margin-bottom: 1rem;">
                                        ${t.images.map((img, i) => `
                                            <div class="task-image-item">
                                                ${t.images.length > 1 ? `<span class="image-option-badge">${i + 1})</span>` : ''}
                                                <img src="${img.url}" style="max-width: 260px; border-radius: 0.5rem; border: 1px solid var(--border-color);">
                                            </div>
                                        `).join('')}
                                    </div>
                                ` : ''}
                                <div style="font-size: 0.9rem;">
                                    <strong>Правильный ответ:</strong> <code style="color: var(--success); font-size: 1rem;">${t.correctAnswer || '—'}</code>
                                </div>
                            </td>
                        </tr>
                    `).join('') || '<tr><td colspan="7" style="padding: 2rem; text-align: center; color: var(--text-secondary);">Задачи по выбранным фильтрам не найдены</td></tr>'}
                </tbody>
            </table>
        </div>

        <!-- Пагинационная плашка -->
        <div class="card" style="padding: 0.8rem 1.2rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.8rem;">
            <div style="font-size: 0.9rem; color: var(--text-secondary);">
                Показано <strong>${filteredTasks.length ? startIndex + 1 : 0}–${endIndex}</strong> из <strong>${filteredTasks.length}</strong> задач
            </div>

            <div style="display: flex; align-items: center; gap: 0.4rem;">
                <button onclick="changeTaskPage(${f.currentPage - 1})" class="btn btn-secondary btn-sm" ${f.currentPage <= 1 ? 'disabled style="opacity:0.5; cursor:not-allowed;"' : ''}>« Назад</button>
                <span style="font-size: 0.9rem; font-weight: 600; padding: 0 0.5rem;">Страница ${f.currentPage} из ${totalPages}</span>
                <button onclick="changeTaskPage(${f.currentPage + 1})" class="btn btn-secondary btn-sm" ${f.currentPage >= totalPages ? 'disabled style="opacity:0.5; cursor:not-allowed;"' : ''}>Вперед »</button>
            </div>
        </div>
    `;
    triggerKaTeX();
}

function changeTaskPage(page) {
    state.taskFilters.currentPage = page;
    loadAdminTasksTab(document.getElementById('admin-tab-content'));
}

function toggleTaskInspector(id) {
    const detailRow = document.getElementById(`task-detail-row-${id}`);
    const btn = document.getElementById(`btn-expand-task-${id}`);
    if (detailRow) {
        if (detailRow.style.display === 'none') {
            detailRow.style.display = 'table-row';
            if (btn) btn.innerHTML = '🔼 Свернуть';
            triggerKaTeX();
        } else {
            detailRow.style.display = 'none';
            if (btn) btn.innerHTML = '👁️ Развернуть';
        }
    }
}

function updateTaskFilterState(key, val) {
    state.taskFilters[key] = val;
    state.taskFilters.currentPage = 1; // Сброс на 1 страницу при изменении любого фильтра
    loadAdminTasksTab(document.getElementById('admin-tab-content'));
}

function renderEditTaskModal(id) {
    const task = state.adminTasks.find(t => t.id === id);
    if (!task) return;

    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1.5rem;">
            <div class="card" style="width: 100%; max-width: 700px; max-height: 90vh; overflow-y: auto;">
                <h2 style="font-size: 1.3rem; margin-bottom: 1rem;">Редактирование задачи (${task.publicId})</h2>

                <form id="edit-task-form">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div class="form-group">
                            <label class="form-label">Предмет</label>
                            <select id="edit-task-subject" class="form-control">
                                ${(state.dictionaries.subjects || []).map(s => `<option value="${s.name}" ${task.subject === s.name ? 'selected' : ''}>${s.displayName}</option>`).join('') || `
                                    <option value="MATHEMATICS" ${task.subject === 'MATHEMATICS' ? 'selected' : ''}>Математика (ОГЭ)</option>
                                    <option value="MATHEMATICS_BASE" ${task.subject === 'MATHEMATICS_BASE' ? 'selected' : ''}>Математика (Базовый уровень)</option>
                                    <option value="MATHEMATICS_PROF" ${task.subject === 'MATHEMATICS_PROF' ? 'selected' : ''}>Математика (Профильный уровень)</option>
                                `}
                            </select>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Тип Экзамена</label>
                            <select id="edit-task-exam" class="form-control">
                                ${(state.dictionaries.exams || []).map(e => `<option value="${e.name}" ${task.examType === e.name ? 'selected' : ''}>${e.displayName}</option>`).join('') || `
                                    <option value="EGE" ${task.examType === 'EGE' ? 'selected' : ''}>ЕГЭ</option>
                                    <option value="OGE" ${task.examType === 'OGE' ? 'selected' : ''}>ОГЭ</option>
                                `}
                            </select>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Банк Заданий</label>
                            <select id="edit-task-bank" class="form-control">
                                ${(state.dictionaries.banks || []).map(b => `<option value="${b.name}" ${task.taskBank === b.name ? 'selected' : ''}>${b.displayName}</option>`).join('') || `
                                    <option value="FIPI" ${task.taskBank === 'FIPI' ? 'selected' : ''}>ФИПИ</option>
                                `}
                            </select>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Номер задания в КИМ</label>
                            <input type="number" id="edit-task-number" class="form-control" value="${task.taskNumber}" min="1" required>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Тема / Подтема</label>
                        <input type="text" id="edit-task-subtopic" class="form-control" value="${task.subtopic || ''}" required>
                    </div>

                    <div class="form-group">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem; flex-wrap: wrap; gap: 0.4rem;">
                            <label class="form-label" style="margin: 0;">Текст задачи (поддерживается LaTeX)</label>
                            <div class="math-toolbar" style="display: flex; gap: 0.3rem; flex-wrap: wrap;">
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('edit-task-content', '\\\\frac{', '}{}', event)" title="Вставить дробь">\frac{a}{b} Дробь</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('edit-task-content', '\\\\sqrt{', '}', event)" title="Вставить корень">\sqrt{x} Корень</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('edit-task-content', '^{', '}', event)" title="Вставить степень">x^a Степень</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('edit-task-content', '\\\\pi', '', event)" title="Вставить Пи">\pi</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('edit-task-content', '\\\\sin(', ')', event)" title="Вставить синус">\sin(x)</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('edit-task-content', '\\\\vec{', '}', event)" title="Вставить вектор">\vec{a}</button>
                            </div>
                        </div>
                        <textarea id="edit-task-content" class="form-control" rows="4" oninput="updateTaskContentPreview(this.value)" required>${task.content || ''}</textarea>
                    </div>

                    <div class="card" style="margin-bottom:1rem; padding:0.8rem; background:var(--bg-hover); border:1px solid var(--border-color);">
                        <div style="font-size:0.8rem; font-weight:600; margin-bottom:0.4rem; color:var(--text-secondary);">👁️ Живое превью формул:</div>
                        <div id="live-task-preview" class="task-content" style="min-height:30px;">${task.content || ''}</div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Правильный ответ</label>
                        <input type="text" id="edit-task-correct-answer" class="form-control" value="${task.correctAnswer || ''}">
                    </div>

                    <div class="form-group">
                        <label class="form-label">Прикрепить новый чертёж / изображение (опционально)</label>
                        <input type="file" id="edit-task-image-file" class="form-control" accept="image/*">
                    </div>

                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Сохранить изменения</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    attachPasteSanitizer('edit-task-content');

    document.getElementById('edit-task-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const subject = document.getElementById('edit-task-subject').value;
        const examType = document.getElementById('edit-task-exam').value;
        const taskBank = document.getElementById('edit-task-bank').value;
        const taskNumber = parseInt(document.getElementById('edit-task-number').value, 10);
        const subtopic = document.getElementById('edit-task-subtopic').value.trim();
        const content = document.getElementById('edit-task-content').value.trim();
        const correctAnswer = document.getElementById('edit-task-correct-answer').value.trim();

        try {
            const updatedTask = await apiFetch(`/api/admin/tasks/${id}`, {
                method: 'PUT',
                body: JSON.stringify({
                    subject, examType, taskBank, taskNumber, subtopic, content, correctAnswer, active: true
                })
            });

            const fileInput = document.getElementById('edit-task-image-file');
            if (fileInput && fileInput.files && fileInput.files.length > 0) {
                const formData = new FormData();
                formData.append('file', fileInput.files[0]);
                await fetch(`/api/admin/tasks/${id}/images`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${state.token || ''}` },
                    body: formData
                });
            }

            closeModal();
            showToast('Задача успешно обновлена!');
            loadAdminTabContent();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });
}

function renderCreateTaskModal() {
    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1.5rem;">
            <div class="card" style="width: 100%; max-width: 700px; max-height: 90vh; overflow-y: auto;">
                <h2 style="font-size: 1.3rem; margin-bottom: 1rem;">Создание новой задачи (ФИПИ / СтатГрад)</h2>

                <form id="create-task-form">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div class="form-group">
                            <label class="form-label">Предмет</label>
                            <select id="task-subject" class="form-control">
                                ${(state.dictionaries.subjects || []).map(s => `<option value="${s.name}">${s.displayName}</option>`).join('') || `
                                    <option value="MATHEMATICS">Математика (ОГЭ)</option>
                                    <option value="MATHEMATICS_BASE">Математика (Базовый уровень)</option>
                                    <option value="MATHEMATICS_PROF">Математика (Профильный уровень)</option>
                                `}
                            </select>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Тип Экзамена</label>
                            <select id="task-exam" class="form-control">
                                ${(state.dictionaries.exams || []).map(e => `<option value="${e.name}">${e.displayName}</option>`).join('') || `
                                    <option value="EGE">ЕГЭ</option>
                                    <option value="OGE">ОГЭ</option>
                                `}
                            </select>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Банк Заданий</label>
                            <select id="task-bank" class="form-control">
                                ${(state.dictionaries.banks || []).map(b => `<option value="${b.name}">${b.displayName}</option>`).join('') || `
                                    <option value="FIPI">ФИПИ</option>
                                `}
                            </select>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Номер задания в КИМ</label>
                            <input type="number" id="task-number" class="form-control" value="1" min="1" required>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Тема / Подтема</label>
                        <input type="text" id="task-subtopic" class="form-control" placeholder="Векторы на плоскости" required>
                    </div>

                    <div class="form-group">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem; flex-wrap: wrap; gap: 0.4rem;">
                            <label class="form-label" style="margin: 0;">Текст задачи (поддерживаются LaTeX $\\vec{a}$ и вставка с ФИПИ)</label>
                            <div class="math-toolbar" style="display: flex; gap: 0.3rem; flex-wrap: wrap;">
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('task-content', '\\\\frac{', '}{}', event)" title="Вставить дробь">\frac{a}{b} Дробь</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('task-content', '\\\\sqrt{', '}', event)" title="Вставить корень">\sqrt{x} Корень</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('task-content', '^{', '}', event)" title="Вставить степень">x^a Степень</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('task-content', '\\\\pi', '', event)" title="Вставить Пи">\pi</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('task-content', '\\\\sin(', ')', event)" title="Вставить синус">\sin(x)</button>
                                <button type="button" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.8rem;" onclick="insertLatexTag('task-content', '\\\\vec{', '}', event)" title="Вставить вектор">\vec{a}</button>
                            </div>
                        </div>
                        <textarea id="task-content" class="form-control" rows="4" placeholder="Даны векторы →a (25; 0)..." oninput="updateTaskContentPreview(this.value)" required></textarea>
                    </div>

                    <div class="card" style="margin-bottom:1rem; padding:0.8rem; background:var(--bg-hover); border:1px solid var(--border-color);">
                        <div style="font-size:0.8rem; font-weight:600; margin-bottom:0.4rem; color:var(--text-secondary);">👁️ Живое превью формул:</div>
                        <div id="live-task-preview" class="task-content" style="min-height:30px;">
                            <span style="color:var(--text-secondary); font-style:italic;">Введите текст задачи или формулы LaTeX ($...$)...</span>
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Правильный ответ (для автопроверки)</label>
                        <input type="text" id="task-correct-answer" class="form-control" placeholder="25">
                    </div>

                    <div class="form-group">
                        <label class="form-label">Прикрепить чертёж / изображение к заданию (опционально)</label>
                        <input type="file" id="task-image-file" class="form-control" accept="image/*">
                    </div>

                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Сохранить задачу</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    attachPasteSanitizer('task-content');

    document.getElementById('create-task-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const subject = document.getElementById('task-subject').value;
        const examType = document.getElementById('task-exam').value;
        const taskBank = document.getElementById('task-bank').value;
        const taskNumber = parseInt(document.getElementById('task-number').value, 10);
        const subtopic = document.getElementById('task-subtopic').value.trim();
        const content = document.getElementById('task-content').value.trim();
        const correctAnswer = document.getElementById('task-correct-answer').value.trim();

        try {
            const savedTask = await apiFetch('/api/admin/tasks', {
                method: 'POST',
                body: JSON.stringify({
                    subject, examType, taskBank, taskNumber, subtopic, content, correctAnswer
                })
            });

            const fileInput = document.getElementById('task-image-file');
            if (fileInput && fileInput.files && fileInput.files.length > 0 && savedTask && savedTask.id) {
                const formData = new FormData();
                formData.append('file', fileInput.files[0]);
                await fetch(`/api/admin/tasks/${savedTask.id}/images`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${state.token || ''}` },
                    body: formData
                });
            }

            closeModal();
            showToast('Задача сохранена!');
            loadAdminTabContent();
        } catch (err) {
            showToast(err.message);
        }
    });
}

async function deleteTask(id) {
    if (!confirm('Удалить эту задачу?')) return;
    try {
        await apiFetch(`/api/admin/tasks/${id}`, { method: 'DELETE' });
        loadAdminTabContent();
    } catch (err) {
        showToast(err.message);
    }
}

// ==========================================
// ADMIN DICTIONARIES & KIM SETTINGS
// ==========================================
async function loadAdminDictionariesTab(container) {
    container.innerHTML = `
        <div style="display: flex; gap: 1.5rem; flex-wrap: wrap;">
            <div class="card" style="flex: 1; min-width: 300px;">
                <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Предметы</h3>
                <div id="dict-subjects-list">Загрузка...</div>
                <hr style="margin: 1rem 0; border: 0; border-top: 1px solid var(--border-color);">
                <form id="add-subject-form">
                    <div class="form-group" style="margin-bottom: 0.5rem;">
                        <label class="form-label" style="font-size: 0.85rem;">Название предмета</label>
                        <input type="text" id="new-subj-display" class="form-control" placeholder="Например: Физика" required>
                    </div>
                    <div class="form-group" style="margin-bottom: 0.8rem;">
                        <label class="form-label" style="font-size: 0.85rem;">Всего заданий в КИМ</label>
                        <input type="number" id="new-subj-totaltasks" class="form-control" value="27" min="1" max="100" required>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">+ Добавить предмет</button>
                </form>
            </div>

            <div class="card" style="flex: 1; min-width: 300px;">
                <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Типы Экзаменов</h3>
                <div id="dict-exams-list">Загрузка...</div>
                <hr style="margin: 1rem 0; border: 0; border-top: 1px solid var(--border-color);">
                <form id="add-exam-form">
                    <div class="form-group" style="margin-bottom: 0.8rem;">
                        <label class="form-label" style="font-size: 0.85rem;">Название экзамена</label>
                        <input type="text" id="new-exam-display" class="form-control" placeholder="Например: ВПР" required>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">+ Добавить экзамен</button>
                </form>
            </div>

            <div class="card" style="flex: 1; min-width: 300px;">
                <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Банки Задач</h3>
                <div id="dict-banks-list">Загрузка...</div>
                <hr style="margin: 1rem 0; border: 0; border-top: 1px solid var(--border-color);">
                <form id="add-bank-form">
                    <div class="form-group" style="margin-bottom: 0.8rem;">
                        <label class="form-label" style="font-size: 0.85rem;">Название банка задач</label>
                        <input type="text" id="new-bank-display" class="form-control" placeholder="Например: РешуЕГЭ" required>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">+ Добавить банк</button>
                </form>
            </div>
        </div>

        <div class="card" style="margin-top: 1.5rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; cursor: pointer;" onclick="toggleKimScoresSection()">
                <h3 style="font-size: 1.2rem; margin: 0;">⚙️ Настройка баллов заданий КИМ</h3>
                <button type="button" id="btn-kim-toggle" class="btn btn-secondary btn-sm">▶ Развернуть</button>
            </div>

            <div id="kim-scores-body" style="display: none; margin-top: 1.2rem;">
                <div style="display: flex; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; align-items: center;">
                    <div>
                        <label class="form-label" style="font-size:0.85rem;">Предмет</label>
                        <select id="kim-subj-select" class="form-control" style="width:240px;" onchange="renderKimScoresGrid()">
                            ${(state.dictionaries.subjects || []).map(s => `<option value="${s.name}">${s.displayName}</option>`).join('') || `
                                <option value="MATHEMATICS_PROF">Математика (Профильный уровень)</option>
                                <option value="MATHEMATICS_BASE">Математика (Базовый уровень)</option>
                                <option value="MATHEMATICS">Математика (ОГЭ)</option>
                            `}
                        </select>
                    </div>
                    <div>
                        <label class="form-label" style="font-size:0.85rem;">Экзамен</label>
                        <select id="kim-exam-select" class="form-control" style="width:140px;" onchange="renderKimScoresGrid()">
                            ${(state.dictionaries.exams || []).map(e => `<option value="${e.name}">${e.displayName}</option>`).join('') || `
                                <option value="EGE">ЕГЭ</option>
                                <option value="OGE">ОГЭ</option>
                            `}
                        </select>
                    </div>
                </div>

                <div id="kim-scores-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 0.8rem;">
                    <p style="color: var(--text-secondary);">Загрузка настроек КИМ...</p>
                </div>
            </div>
        </div>
    `;

    loadDictionariesLists();

    document.getElementById('add-subject-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const displayName = document.getElementById('new-subj-display').value.trim();
        const totalTasks = parseInt(document.getElementById('new-subj-totaltasks').value, 10);
        try {
            await apiFetch('/api/admin/dictionaries/subjects', {
                method: 'POST',
                body: JSON.stringify({ displayName, totalTasks })
            });
            loadDictionariesLists();
            showToast('Предмет добавлен!');
        } catch (err) { showToast(err.message, 'error'); }
    });

    document.getElementById('add-exam-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const displayName = document.getElementById('new-exam-display').value.trim();
        try {
            await apiFetch('/api/admin/dictionaries/exams', {
                method: 'POST',
                body: JSON.stringify({ displayName })
            });
            loadDictionariesLists();
            showToast('Тип экзамена добавлен!');
        } catch (err) { showToast(err.message, 'error'); }
    });

    document.getElementById('add-bank-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const displayName = document.getElementById('new-bank-display').value.trim();
        try {
            await apiFetch('/api/admin/dictionaries/banks', {
                method: 'POST',
                body: JSON.stringify({ displayName })
            });
            loadDictionariesLists();
            showToast('Банк задач добавлен!');
        } catch (err) { showToast(err.message, 'error'); }
    });
}

function toggleKimScoresSection() {
    const body = document.getElementById('kim-scores-body');
    const btn = document.getElementById('btn-kim-toggle');
    if (body) {
        if (body.style.display === 'none') {
            body.style.display = 'block';
            if (btn) btn.innerHTML = '🔼 Свернуть';
            renderKimScoresGrid();
        } else {
            body.style.display = 'none';
            if (btn) btn.innerHTML = '▶ Развернуть';
        }
    }
}

async function renderKimScoresGrid() {
    const grid = document.getElementById('kim-scores-grid');
    if (!grid) return;

    const subjectSelect = document.getElementById('kim-subj-select');
    const examSelect = document.getElementById('kim-exam-select');
    const subject = subjectSelect ? subjectSelect.value : 'MATHEMATICS';
    const exam = examSelect ? examSelect.value : 'EGE';

    try {
        const settings = await apiFetch(`/api/admin/dictionaries/kim-settings?subject=${subject}&exam=${exam}`);
        const scoreMap = {};
        if (Array.isArray(settings)) {
            settings.forEach(s => scoreMap[s.taskNumber] = s.maxScore);
        }

        let totalTasks = 27;
        if (state.dictionaries && state.dictionaries.subjects) {
            const subjObj = state.dictionaries.subjects.find(s => s.name === subject);
            if (subjObj && subjObj.totalTasks) totalTasks = subjObj.totalTasks;
        }

        let html = '';
        for (let i = 1; i <= totalTasks; i++) {
            const score = scoreMap[i] || 1;
            html += `
                <div style="padding: 0.6rem; background: var(--bg-hover); border-radius: 0.5rem; text-align: center; border: 1px solid var(--border-color);">
                    <div style="font-size: 0.85rem; font-weight: 600; margin-bottom: 0.4rem; color: var(--text-secondary);">№ ${i}</div>
                    <div style="display: flex; justify-content: center; align-items: center; gap: 0.4rem;">
                        <button onclick="updateKimScore('${subject}', '${exam}', ${i}, ${score - 1})" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-weight: 700;">-</button>
                        <span style="font-weight: 700; font-size: 1rem; min-width: 20px;">${score}</span>
                        <button onclick="updateKimScore('${subject}', '${exam}', ${i}, ${score + 1})" class="btn btn-sm btn-secondary" style="padding: 0.15rem 0.5rem; font-weight: 700;">+</button>
                    </div>
                </div>
            `;
        }
        grid.innerHTML = html;
    } catch (err) {
        grid.innerHTML = `<span class="badge badge-danger">Ошибка загрузки: ${err.message}</span>`;
    }
}

async function updateKimScore(subject, exam, taskNumber, maxScore) {
    if (maxScore < 1) maxScore = 1;
    try {
        await apiFetch('/api/admin/dictionaries/kim-settings', {
            method: 'POST',
            body: JSON.stringify({ subject, exam, taskNumber, maxScore })
        });
        renderKimScoresGrid();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadDictionariesLists() {
    const subjList = document.getElementById('dict-subjects-list');
    const examList = document.getElementById('dict-exams-list');
    const bankList = document.getElementById('dict-banks-list');

    try {
        const subjects = await apiFetch('/api/admin/dictionaries/subjects');
        const exams = await apiFetch('/api/admin/dictionaries/exams');
        const banks = await apiFetch('/api/admin/dictionaries/banks');

        state.dictionaries = { subjects, exams, banks };

        if (subjList) {
            subjList.innerHTML = subjects.map(s => `
                <div style="padding: 0.5rem 0; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; gap: 0.5rem;">
                    <div><strong>${s.displayName}</strong></div>
                    <div style="display: flex; gap: 0.4rem; align-items: center;">
                        <span class="badge badge-info">${s.totalTasks || 27} заданий в КИМ</span>
                        <button onclick="renderEditDictionaryItemModal('subjects', ${s.id}, '${s.displayName}', ${s.totalTasks || 27})" class="btn btn-sm btn-secondary" title="Редактировать">✏️</button>
                        <button onclick="deleteDictionaryItem('subjects', ${s.id})" class="btn btn-sm btn-danger" title="Удалить">🗑️</button>
                    </div>
                </div>
            `).join('') || '<p style="color: var(--text-secondary);">Предметы отсутствуют</p>';
        }

        if (examList) {
            examList.innerHTML = exams.map(e => `
                <div style="padding: 0.5rem 0; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; gap: 0.5rem;">
                    <div><strong>${e.displayName}</strong></div>
                    <div style="display: flex; gap: 0.4rem; align-items: center;">
                        <button onclick="renderEditDictionaryItemModal('exams', ${e.id}, '${e.displayName}')" class="btn btn-sm btn-secondary" title="Редактировать">✏️</button>
                        <button onclick="deleteDictionaryItem('exams', ${e.id})" class="btn btn-sm btn-danger" title="Удалить">🗑️</button>
                    </div>
                </div>
            `).join('') || '<p style="color: var(--text-secondary);">Экзамены отсутствуют</p>';
        }

        if (bankList) {
            bankList.innerHTML = banks.map(b => `
                <div style="padding: 0.5rem 0; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; gap: 0.5rem;">
                    <div><strong>${b.displayName}</strong></div>
                    <div style="display: flex; gap: 0.4rem; align-items: center;">
                        <button onclick="renderEditDictionaryItemModal('banks', ${b.id}, '${b.displayName}')" class="btn btn-sm btn-secondary" title="Редактировать">✏️</button>
                        <button onclick="deleteDictionaryItem('banks', ${b.id})" class="btn btn-sm btn-danger" title="Удалить">🗑️</button>
                    </div>
                </div>
            `).join('') || '<p style="color: var(--text-secondary);">Банки задач отсутствуют</p>';
        }

        const kimSubjSelect = document.getElementById('kim-subj-select');
        if (kimSubjSelect && subjects.length) {
            const currentSubj = kimSubjSelect.value;
            kimSubjSelect.innerHTML = subjects.map(s => `<option value="${s.name}" ${s.name === currentSubj ? 'selected' : ''}>${s.displayName}</option>`).join('');
        }
        const kimExamSelect = document.getElementById('kim-exam-select');
        if (kimExamSelect && exams.length) {
            const currentExam = kimExamSelect.value;
            kimExamSelect.innerHTML = exams.map(e => `<option value="${e.name}" ${e.name === currentExam ? 'selected' : ''}>${e.displayName}</option>`).join('');
        }
        triggerKaTeX();
    } catch (err) {
        console.error('Error loading dictionaries:', err);
        if (subjList) subjList.innerHTML = `<span class="badge badge-danger">Ошибка: ${err.message}</span>`;
        if (examList) examList.innerHTML = `<span class="badge badge-danger">Ошибка: ${err.message}</span>`;
        if (bankList) bankList.innerHTML = `<span class="badge badge-danger">Ошибка: ${err.message}</span>`;
    }
}

function renderEditDictionaryItemModal(type, id, currentDisplayName, currentTotalTasks) {
    const titleMap = { subjects: 'предмета', exams: 'типа экзамена', banks: 'банка задач' };
    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div class="card" style="width: 100%; max-width: 400px;">
                <h2 style="font-size: 1.2rem; margin-bottom: 1rem;">Редактирование ${titleMap[type] || 'элемента'}</h2>
                <form id="edit-dict-form">
                    <div class="form-group">
                        <label class="form-label">Название</label>
                        <input type="text" id="edit-dict-display" class="form-control" value="${currentDisplayName || ''}" required>
                    </div>
                    ${type === 'subjects' ? `
                    <div class="form-group">
                        <label class="form-label">Всего заданий в КИМ</label>
                        <input type="number" id="edit-dict-totaltasks" class="form-control" value="${currentTotalTasks || 27}" min="1" max="100" required>
                    </div>
                    ` : ''}
                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Сохранить</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    document.getElementById('edit-dict-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const displayName = document.getElementById('edit-dict-display').value.trim();
        const bodyObj = { displayName };
        if (type === 'subjects') {
            bodyObj.totalTasks = parseInt(document.getElementById('edit-dict-totaltasks').value, 10);
        }
        try {
            await apiFetch(`/api/admin/dictionaries/${type}/${id}`, {
                method: 'PUT',
                body: JSON.stringify(bodyObj)
            });
            closeModal();
            showToast('Изменения сохранены!');
            loadDictionariesLists();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });
}

async function deleteDictionaryItem(type, id) {
    if (!confirm('Удалить этот элемент из справочника?')) return;
    try {
        await apiFetch(`/api/admin/dictionaries/${type}/${id}`, { method: 'DELETE' });
        showToast('Элемент удален из справочника');
        loadDictionariesLists();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================
// ADMIN MONITORING DASHBOARD
// ==========================================
async function loadAdminMonitoringTab(container) {
    container.innerHTML = '<p style="text-align:center; padding: 2rem;">Сбор показателей сервера...</p>';
    try {
        const metrics = await apiFetch('/api/admin/monitoring');

        const ramPercent = metrics.totalMemoryMb ? Math.round((metrics.usedMemoryMb / metrics.totalMemoryMb) * 100) : 0;
        const diskPercent = metrics.totalDiskGb ? Math.round(((metrics.totalDiskGb - metrics.freeDiskGb) / metrics.totalDiskGb) * 100) : 0;
        const uptimeSec = metrics.systemUptimeSeconds !== undefined ? metrics.systemUptimeSeconds : (metrics.uptimeSeconds !== undefined ? metrics.uptimeSeconds : 0);
        const activeConn = metrics.activeConnectionsCount !== undefined ? metrics.activeConnectionsCount : (metrics.activeConnections !== undefined ? metrics.activeConnections : 0);
        const hours = Math.floor(uptimeSec / 3600);
        const mins = Math.floor((uptimeSec % 3600) / 60);

        container.innerHTML = `
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">Загрузка Процессора (CPU)</div>
                    <div class="metric-value">${metrics.cpuUsagePercent}%</div>
                    <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: ${metrics.cpuUsagePercent}%;"></div></div>
                    <div style="font-size:0.8rem; margin-top:0.4rem; color:var(--text-secondary);">Ядер доступно: ${metrics.availableProcessors}</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Оперативная память (RAM)</div>
                    <div class="metric-value">${metrics.usedMemoryMb} MB</div>
                    <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: ${ramPercent}%;"></div></div>
                    <div style="font-size:0.8rem; margin-top:0.4rem; color:var(--text-secondary);">Из ${metrics.totalMemoryMb} MB (${ramPercent}%)</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Дисковое пространство</div>
                    <div class="metric-value">${metrics.freeDiskGb} GB своб.</div>
                    <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: ${diskPercent}%;"></div></div>
                    <div style="font-size:0.8rem; margin-top:0.4rem; color:var(--text-secondary);">Всего: ${metrics.totalDiskGb} GB</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Аптайм & Подключения</div>
                    <div class="metric-value">${Math.floor(metrics.systemUptimeSeconds / 3600)} ч. ${Math.floor((metrics.systemUptimeSeconds % 3600) / 60)} м.</div>
                    <div style="font-size:0.9rem; margin-top:0.5rem; color:var(--text-primary);">Активных сессий: <strong>${metrics.activeConnectionsCount}</strong></div>
                    <div style="margin-top: 0.5rem;">
                        <span class="badge badge-success">Docker: Running</span>
                        <span class="badge badge-success">App: Healthy</span>
                    </div>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="badge badge-danger">Ошибка мониторинга: ${err.message}</div>`;
    }
}

// ==========================================
// TEACHER DASHBOARD VIEW
// ==========================================
async function renderTeacherDashboard() {
    const app = document.getElementById('app');
    app.innerHTML = `
        <nav class="navbar">
            <a href="#teacher" class="navbar-brand">Reshaemo <span class="logo-tag">Преподаватель</span></a>
            <div class="navbar-user">
                <span class="user-badge">${state.user.displayName}</span>
                <button onclick="logout()" class="btn btn-secondary btn-sm">Выйти</button>
            </div>
        </nav>

        <div class="container">
            <div class="tabs">
                <button class="tab-btn ${state.teacherTab === 'variants' ? 'active' : ''}" onclick="switchTeacherTab('variants', event)">Мои Тесты</button>
                <button class="tab-btn ${state.teacherTab === 'create-variant' ? 'active' : ''}" onclick="switchTeacherTab('create-variant', event)">Собрать новый вариант</button>
                <button class="tab-btn ${state.teacherTab === 'students' ? 'active' : ''}" onclick="switchTeacherTab('students', event)">Ученики & Результаты</button>
                <button class="tab-btn ${state.teacherTab === 'profile' ? 'active' : ''}" onclick="switchTeacherTab('profile', event)">Профиль</button>
            </div>

            <div id="teacher-tab-content"></div>
        </div>
    `;

    loadTeacherTabContent();
}

function switchTeacherTab(tab, evt) {
    state.teacherTab = tab;
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    if (evt && evt.target) {
        evt.target.classList.add('active');
    }
    loadTeacherTabContent();
}


async function loadTeacherTabContent() {
    const container = document.getElementById('teacher-tab-content');
    if (state.teacherTab === 'variants') {
        await loadTeacherVariantsTab(container);
    } else if (state.teacherTab === 'create-variant') {
        await renderTeacherCreateVariantView(container);
    } else if (state.teacherTab === 'students') {
        await loadTeacherStudentsTab(container);
    } else if (state.teacherTab === 'profile') {
        renderTeacherProfileTab(container);
    }
}

// ------------------------------------------
// TEACHER: VARIANTS LIST
// ------------------------------------------
async function loadTeacherVariantsTab(container) {
    container.innerHTML = '<p style="text-align:center; padding: 2rem;">Загрузка списка созданных тестов...</p>';
    try {
        const variants = await apiFetch('/api/teacher/variants');
        state.teacherVariants = variants;

        container.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h2 style="font-size: 1.4rem; font-weight: 700;">Созданные тесты (${variants.length})</h2>
                <button onclick="switchTeacherTab('create-variant')" class="btn btn-primary">+ Собрать тест</button>
            </div>

            <div class="variant-grid">
                ${variants.map(v => {
                    const testUrl = `${window.location.origin}${window.location.pathname}#test/${v.accessToken}`;
                    return `
                        <div class="card">
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.8rem;">
                                <span class="badge badge-info">${v.subject} — ${v.examType}</span>
                                <span style="font-size: 0.8rem; color: var(--text-secondary);">${new Date(v.createdAt).toLocaleDateString('ru-RU')}</span>
                            </div>

                            <h3 style="font-size: 1.2rem; font-weight: 700; margin-bottom: 0.8rem;">${v.title}</h3>

                            <div style="display: flex; gap: 1.5rem; margin-bottom: 1.2rem; font-size: 0.9rem; color: var(--text-secondary);">
                                <div>Заданий: <strong>${v.taskCount}</strong></div>
                                <div>Сдало учеников: <strong>${v.studentAttemptsCount !== undefined ? v.studentAttemptsCount : (v.attemptsCount !== undefined ? v.attemptsCount : 0)}</strong></div>
                                <div>Ср. балл: <strong>${v.averageScorePercent !== null && v.averageScorePercent !== undefined ? v.averageScorePercent + '%' : '—'}</strong></div>
                            </div>

                            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                                <button onclick="copyToClipboard('${testUrl}', this)" class="btn btn-sm btn-primary">Ссылка для учеников</button>
                                <button onclick="window.open('/print-variant.html?id=${v.id}&token=' + encodeURIComponent(state.token || ''), '_blank')" class="btn btn-sm btn-secondary">Печать / PDF</button>
                                <button onclick="renderVariantStatsModalById(${v.id})" class="btn btn-sm btn-secondary">Результаты</button>
                                <button onclick="deleteVariant(${v.id})" class="btn btn-sm btn-danger">Удалить</button>
                            </div>
                        </div>
                    `;
                }).join('') || '<div class="card" style="grid-column: 1/-1; text-align: center; padding: 3rem;"><p>Вы еще не создали ни одного теста.</p></div>'}
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="badge badge-danger">Ошибка: ${err.message}</div>`;
    }
}

// ------------------------------------------
// TEACHER: CREATE VARIANT (Subtopic Task Builder)
// ------------------------------------------
async function renderTeacherCreateVariantView(container) {
    container.innerHTML = '<p style="text-align:center; padding: 2rem;">Загрузка банка вопросов...</p>';
    try {
        const [tasks, subjects, exams, banks] = await Promise.all([
            apiFetch('/api/admin/tasks'),
            apiFetch('/api/admin/dictionaries/subjects').catch(() => []),
            apiFetch('/api/admin/dictionaries/exams').catch(() => []),
            apiFetch('/api/admin/dictionaries/banks').catch(() => [])
        ]);

        state.adminTasks = tasks;
        if (subjects.length) state.dictionaries.subjects = subjects;
        if (exams.length) state.dictionaries.exams = exams;
        if (banks.length) state.dictionaries.banks = banks;

        let selectedSubject = 'MATHEMATICS';
        let selectedExam = 'EGE';
        let selectedBank = 'FIPI';

        function renderBuilder() {
            const filteredTasks = state.adminTasks.filter(t => 
                t.subject === selectedSubject && t.examType === selectedExam && t.taskBank === selectedBank
            );

            // Group tasks by taskNumber -> subtopic
            const grouped = {};
            filteredTasks.forEach(t => {
                const num = t.taskNumber || 1;
                const sub = t.subtopic || 'Общие задания';
                if (!grouped[num]) grouped[num] = {};
                if (!grouped[num][sub]) grouped[num][sub] = [];
                grouped[num][sub].push(t);
            });

            container.innerHTML = `
                <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 1.5rem;">
                    <!-- LEFT COLUMN: Task Bank Subtopics -->
                    <div>
                        <div class="card" style="margin-bottom: 1.5rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                                <h2 style="font-size: 1.3rem; margin: 0;">Параметры варианта</h2>
                                <button type="button" onclick="generateRandomVariant()" class="btn btn-secondary btn-sm" style="font-weight: 600;">
                                    🎲 Случайный вариант
                                </button>
                            </div>
                            
                            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem;">
                                <div>
                                    <label class="form-label">Предмет</label>
                                    <select id="builder-subject" class="form-control">
                                        ${state.dictionaries.subjects.map(s => `<option value="${s.name}" ${selectedSubject === s.name ? 'selected' : ''}>${s.displayName}</option>`).join('')}
                                    </select>
                                </div>
                                <div>
                                    <label class="form-label">Экзамен</label>
                                    <select id="builder-exam" class="form-control">
                                        ${state.dictionaries.exams.map(e => `<option value="${e.name}" ${selectedExam === e.name ? 'selected' : ''}>${e.displayName}</option>`).join('')}
                                    </select>
                                </div>
                                <div>
                                    <label class="form-label">Банк задач</label>
                                    <select id="builder-bank" class="form-control">
                                        ${state.dictionaries.banks.map(b => `<option value="${b.name}" ${selectedBank === b.name ? 'selected' : ''}>${b.displayName}</option>`).join('')}
                                    </select>
                                </div>
                            </div>
                        </div>

                        <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Темы и подтемы задач в банке</h3>
                        <p style="color: var(--text-secondary); font-size: 0.85rem; margin-bottom: 1rem;">Выберите количество задач из подтем для включения в контрольный вариант.</p>

                        ${Object.keys(grouped).sort((a,b) => parseInt(a) - parseInt(b)).map(taskNum => `
                            <div class="card" style="margin-bottom: 1rem;">
                                <div style="font-weight: 700; font-size: 1.1rem; color: var(--primary); margin-bottom: 0.8rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.4rem;">
                                    Задание №${taskNum} КИМ
                                </div>

                                ${Object.keys(grouped[taskNum]).map(subtopic => {
                                    const available = grouped[taskNum][subtopic];
                                    const addedIds = available.filter(t => state.selectedTaskIds.includes(t.id)).map(t => t.id);
                                    const addedCount = addedIds.length;

                                    return `
                                        <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px dashed var(--border-color);">
                                            <div>
                                                <div style="font-weight: 600;">${subtopic}</div>
                                                <div style="font-size: 0.8rem; color: var(--text-secondary);">Доступно задач: ${available.length} | 1 первичный балл</div>
                                            </div>

                                            <div style="display: flex; align-items: center; gap: 0.5rem;">
                                                <button type="button" class="btn btn-sm btn-secondary" onclick="removeSubtopicTask('${taskNum}', '${subtopic.replace(/'/g, "\\'")}')" ${addedCount === 0 ? 'disabled' : ''}>-</button>
                                                <span style="font-weight: 700; min-width: 25px; text-align: center;">${addedCount}</span>
                                                <button type="button" class="btn btn-sm btn-primary" onclick="addSubtopicTask('${taskNum}', '${subtopic.replace(/'/g, "\\'")}')" ${addedCount >= available.length ? 'disabled' : ''}>+</button>
                                            </div>
                                        </div>
                                    `;
                                }).join('')}
                            </div>
                        `).join('') || '<div class="card"><p style="text-align: center;">Задачи с указанными фильтрами отсутствуют.</p></div>'}
                    </div>

                    <!-- RIGHT COLUMN: Selected Tasks Summary -->
                    <div>
                        <div class="card" style="position: sticky; top: 80px;">
                            <h3 style="font-size: 1.2rem; margin-bottom: 1rem;">Состав варианта (${state.selectedTaskIds.length})</h3>

                            <div class="form-group">
                                <label class="form-label">Название варианта</label>
                                <input type="text" id="variant-title-input" class="form-control" placeholder="Контрольная работа №1" value="Вариант по математике (${selectedExam})">
                            </div>

                            <div style="max-height: 350px; overflow-y: auto; margin-bottom: 1rem;">
                                ${state.selectedTaskIds.map((id, index) => {
                                    const task = state.adminTasks.find(t => t.id === id);
                                    if (!task) return '';
                                    return `
                                        <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.5rem; background: var(--bg-hover); border-radius: var(--radius); margin-bottom: 0.4rem; font-size: 0.85rem;">
                                            <div>
                                                <strong>№${index + 1}.</strong> [Задание №${task.taskNumber}] ${task.subtopic}
                                            </div>
                                            <button onclick="removeSelectedTaskId(${id})" style="background:none; border:none; color:var(--danger); cursor:pointer; font-weight:bold;">✕</button>
                                        </div>
                                    `;
                                }).join('') || '<p style="color: var(--text-secondary); text-align: center; padding: 1rem;">Вариант пока пуст. Используйте кнопки + слева.</p>'}
                            </div>

                            <button onclick="submitCreateVariant('${selectedSubject}', '${selectedExam}', '${selectedBank}')" class="btn btn-primary" style="width: 100%;" ${state.selectedTaskIds.length === 0 ? 'disabled' : ''}>
                                ✅ Сгенерировать вариант (${state.selectedTaskIds.length} зад.)
                            </button>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById('builder-subject').addEventListener('change', (e) => { 
                selectedSubject = e.target.value; 
                if (selectedSubject === 'MATHEMATICS') selectedExam = 'OGE';
                else if (selectedSubject === 'MATHEMATICS_BASE' || selectedSubject === 'MATHEMATICS_PROF') selectedExam = 'EGE';
                renderBuilder(); 
            });
            document.getElementById('builder-exam').addEventListener('change', (e) => { selectedExam = e.target.value; renderBuilder(); });
            document.getElementById('builder-bank').addEventListener('change', (e) => { selectedBank = e.target.value; renderBuilder(); });
        }

        window.addSubtopicTask = (taskNum, subtopic) => {
            const available = state.adminTasks.filter(t => 
                t.subject === selectedSubject && t.examType === selectedExam && t.taskBank === selectedBank &&
                t.taskNumber == taskNum && t.subtopic === subtopic
            );
            const unadded = available.filter(t => !state.selectedTaskIds.includes(t.id));
            if (unadded.length > 0) {
                const randomTask = unadded[Math.floor(Math.random() * unadded.length)];
                state.selectedTaskIds.push(randomTask.id);
                renderBuilder();
            }
        };

        window.generateRandomVariant = () => {
            state.selectedTaskIds = [];
            const filteredTasks = state.adminTasks.filter(t => 
                t.subject === selectedSubject && t.examType === selectedExam && t.taskBank === selectedBank
            );

            // Группируем доступные задачи по номерам КИМ (taskNumber)
            const byNumber = {};
            filteredTasks.forEach(t => {
                const num = t.taskNumber || 1;
                if (!byNumber[num]) byNumber[num] = [];
                byNumber[num].push(t);
            });

            // Берём ровно по 1 случайной задаче на каждый номер КИМ
            Object.keys(byNumber).sort((a,b) => parseInt(a) - parseInt(b)).forEach(num => {
                const tasksForNum = byNumber[num];
                if (tasksForNum.length > 0) {
                    const randomTask = tasksForNum[Math.floor(Math.random() * tasksForNum.length)];
                    state.selectedTaskIds.push(randomTask.id);
                }
            });

            if (state.selectedTaskIds.length > 0) {
                showToast(`Сформирован случайный вариант из ${state.selectedTaskIds.length} задач!`);
            } else {
                showToast('В банке нет задач по выбранным фильтрам');
            }
            renderBuilder();
        };

        window.removeSubtopicTask = (taskNum, subtopic) => {
            const available = state.adminTasks.filter(t => 
                t.subject === selectedSubject && t.examType === selectedExam && t.taskBank === selectedBank &&
                t.taskNumber == taskNum && t.subtopic === subtopic
            );
            const added = available.filter(t => state.selectedTaskIds.includes(t.id));
            if (added.length > 0) {
                const lastId = added[added.length - 1].id;
                state.selectedTaskIds = state.selectedTaskIds.filter(id => id !== lastId);
                renderBuilder();
            }
        };

        window.removeSelectedTaskId = (id) => {
            state.selectedTaskIds = state.selectedTaskIds.filter(tId => tId !== id);
            renderBuilder();
        };

        renderBuilder();
    } catch (err) {
        container.innerHTML = `<div class="badge badge-danger">Ошибка: ${err.message}</div>`;
    }
}

async function submitCreateVariant(subject, examType, taskBank) {
    const title = document.getElementById('variant-title-input').value.trim() || 'Новый вариант';
    try {
        const res = await apiFetch('/api/teacher/variants', {
            method: 'POST',
            body: JSON.stringify({
                title, subject, examType, taskBank, taskIds: state.selectedTaskIds
            })
        });

        state.selectedTaskIds = [];
        showToast('Вариант успешно сгенерирован!');
        switchTeacherTab('variants');
    } catch (err) {
        showToast(err.message);
    }
}

// ------------------------------------------
// TEACHER: STUDENTS TRACKING & RETAKES
// ------------------------------------------
async function loadTeacherStudentsTab(container) {
    container.innerHTML = '<p style="text-align:center; padding: 2rem;">Загрузка данных учеников...</p>';
    try {
        const students = await apiFetch('/api/teacher/students');
        state.teacherStudents = students;

        container.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                <h2 style="font-size: 1.4rem; font-weight: 700;">Ученики и активность (${students.length})</h2>
            </div>

            <div class="card" style="padding: 0; overflow: hidden;">
                <table style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr style="background: var(--bg-hover); text-align: left;">
                            <th style="padding: 1rem;">ФИО Ученика</th>
                            <th style="padding: 1rem;">Digital Fingerprint</th>
                            <th style="padding: 1rem;">Пройдено тестов</th>
                            <th style="padding: 1rem;">Средний результат</th>
                            <th style="padding: 1rem;">Последняя активность</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${students.map(s => `
                            <tr style="border-top: 1px solid var(--border-color);">
                                <td style="padding: 1rem;"><strong>${s.displayName}</strong></td>
                                <td style="padding: 1rem;"><code>${s.browserFingerprint}</code></td>
                                <td style="padding: 1rem;">${s.totalAttempts}</td>
                                <td style="padding: 1rem;">
                                    ${s.averageScorePercent !== null ? `<span class="badge badge-success">${s.averageScorePercent}%</span>` : '—'}
                                </td>
                                <td style="padding: 1rem;">${s.lastActivityDate ? new Date(s.lastActivityDate).toLocaleString('ru-RU') : '—'}</td>
                            </tr>
                        `).join('') || '<tr><td colspan="5" style="padding: 2rem; text-align: center;">Ученики пока не проходили тесты</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="badge badge-danger">Ошибка: ${err.message}</div>`;
    }
}

// ------------------------------------------
// TEACHER: PROFILE TAB
// ------------------------------------------
function renderTeacherProfileTab(container) {
    container.innerHTML = `
        <div class="card" style="max-width: 600px; margin: 0 auto;">
            <h2 style="font-size: 1.4rem; margin-bottom: 1.5rem;">Личный кабинет</h2>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem;">
                <div>
                    <label class="form-label">ФИО Преподавателя</label>
                    <div style="font-weight: 600; font-size: 1.1rem;">${state.user.displayName}</div>
                </div>
                <div>
                    <label class="form-label">Логин</label>
                    <div style="font-weight: 600; font-size: 1.1rem;"><code>${state.user.login}</code></div>
                </div>
            </div>

            <div style="margin-bottom: 1.5rem;">
                <label class="form-label">Статус подписки</label>
                <div><span class="badge badge-success">Активна (доступ без ограничений)</span></div>
            </div>

            <button onclick="renderChangePasswordModal()" class="btn btn-secondary">Изменить пароль</button>
        </div>
    `;
}

function renderChangePasswordModal() {
    const modalHtml = `
        <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div class="card" style="width: 100%; max-width: 420px;">
                <h2 style="font-size: 1.3rem; margin-bottom: 1rem;">Изменение пароля</h2>

                <form id="change-password-form">
                    <div class="form-group">
                        <label class="form-label">Текущий пароль</label>
                        <input type="password" id="old-pass" data-mask="password" class="form-control" required>
                    </div>

                    <div class="form-group">
                        <label class="form-label">Новый пароль (только латиница)</label>
                        <input type="password" id="new-pass" data-mask="password" class="form-control" placeholder="••••••••" required>
                    </div>

                    <div style="display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Отмена</button>
                        <button type="submit" class="btn btn-primary">Сохранить новый пароль</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    applyInputMasks();

    document.getElementById('change-password-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const currentPassword = document.getElementById('old-pass').value;
        const newPassword = document.getElementById('new-pass').value;

        try {
            await apiFetch('/api/auth/change-password', {
                method: 'POST',
                body: JSON.stringify({ currentPassword, newPassword })
            });

            closeModal();
            showToast('Пароль успешно изменён!');
        } catch (err) {
            showToast(err.message);
        }
    });
}

// ------------------------------------------
// VARIANT STATS MODAL & RETAKE PERMISSION
// ------------------------------------------
async function renderVariantStatsModalById(id) {
    try {
        const stats = await apiFetch(`/api/teacher/variants/${id}/stats`);
        const modalHtml = `
            <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1.5rem;">
                <div class="card" style="width: 100%; max-width: 750px; max-height: 90vh; overflow-y: auto;">
                    <h2 style="font-size: 1.4rem; margin-bottom: 0.5rem;">Результаты выполнения теста</h2>
                    <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">Вариант: <strong>${stats.title}</strong></p>

                    <div style="padding: 0; overflow: hidden; margin-bottom: 1rem;">
                        <table style="width: 100%; border-collapse: collapse;">
                            <thead>
                                <tr style="background: var(--bg-hover); text-align: left;">
                                    <th style="padding: 0.8rem;">ФИО Ученика</th>
                                    <th style="padding: 0.8rem;">Дата сдачи</th>
                                    <th style="padding: 0.8rem;">Балл (%)</th>
                                    <th style="padding: 0.8rem;">Пересдача</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${stats.attempts.map(a => `
                                    <tr style="border-top: 1px solid var(--border-color);">
                                        <td style="padding: 0.8rem;"><strong>${a.studentName}</strong></td>
                                        <td style="padding: 0.8rem;">${a.completedAt ? new Date(a.completedAt).toLocaleString('ru-RU') : 'В процессе'}</td>
                                        <td style="padding: 0.8rem;">
                                            ${a.scorePercent !== null ? `<span class="badge badge-success">${a.scorePercent}%</span>` : '<span class="badge badge-warning">Не завершено</span>'}
                                        </td>
                                        <td style="padding: 0.8rem; display: flex; gap: 0.4rem; align-items: center;">
                                            <button onclick="renderAttemptGradingModal(${id}, ${a.attemptId})" class="btn btn-sm btn-primary" title="Проверить развернутые ответы и выставить балл">Проверить</button>
                                            <button onclick="allowStudentRetake(${id}, ${a.studentId})" class="btn btn-sm btn-secondary" title="Разрешить пересдачу">Разрешить пересдачу</button>
                                        </td>
                                    </tr>
                                `).join('') || '<tr><td colspan="4" style="padding: 1.5rem; text-align: center;">Ни один ученик ещё не сдал этот тест.</td></tr>'}
                            </tbody>
                        </table>
                    </div>

                    <div style="display:flex; justify-content: flex-end; margin-top: 1rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Закрыть</button>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
    } catch (err) {
        showToast(err.message);
    }
}

async function renderAttemptGradingModal(variantId, attemptId) {
    try {
        const details = await apiFetch(`/api/teacher/variants/${variantId}/attempts/${attemptId}`);

        // Separate auto-graded (has correctAnswer) from open-ended tasks
        const answers = details.answers || [];
        const openAnswers = answers.filter(ans => !ans.correctAnswer);
        const autoAnswers = answers.filter(ans => !!ans.correctAnswer);

        // Build per-task score inputs HTML for open-ended tasks
        const openInputsHtml = openAnswers.length ? `
            <div style="margin-bottom: 1.2rem;">
                <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 0.8rem; color: var(--text-secondary);">📝 Открытые задания — выставьте баллы:</h3>
                ${openAnswers.map(ans => `
                    <div style="padding: 0.8rem; border: 1px solid var(--border-color); border-radius: 0.6rem; margin-bottom: 0.8rem; background: var(--bg-hover);">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                            <strong>Задание №${ans.itemIndex} <code style="font-size:0.8rem;">[${ans.publicId}] №${ans.taskNumber} КИМ</code></strong>
                            <span class="badge badge-warning">Макс. балл: ${ans.maxScore || 1}</span>
                        </div>
                        <div class="task-content" style="margin-bottom: 0.6rem; padding: 0.5rem; background: var(--bg-card); border-radius: 0.3rem; font-size: 0.9rem;">${ans.content}</div>
                        ${(ans.imageUrls && ans.imageUrls.length) ? `
                            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.6rem;">
                                ${ans.imageUrls.map(url => `<img src="${url}" style="max-height: 120px; border-radius: 0.3rem; border: 1px solid var(--border-color);">`).join('')}
                            </div>
                        ` : ''}
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.8rem; font-size: 0.9rem; margin-bottom: 0.6rem;">
                            <div>
                                <div style="font-weight: 600; color: var(--text-secondary); margin-bottom: 0.2rem;">Ответ ученика:</div>
                                <div style="padding: 0.4rem; background: var(--bg-main); border-radius: 0.3rem; font-family: monospace;">${ans.givenAnswer || '<em>Не предоставлен</em>'}</div>
                            </div>
                            <div>
                                <div style="font-weight: 600; color: var(--text-secondary); margin-bottom: 0.2rem;">Выставить балл (0 – ${ans.maxScore || 1}):</div>
                                <input type="number" id="task-score-${ans.taskId}" data-task-id="${ans.taskId}" data-max-score="${ans.maxScore || 1}"
                                    class="form-control task-score-input" style="width: 90px;"
                                    value="${ans.manualScore !== null && ans.manualScore !== undefined ? ans.manualScore : 0}"
                                    min="0" max="${ans.maxScore || 1}" step="1">
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        ` : '';

        const autoSummaryHtml = autoAnswers.length ? `
            <div style="margin-bottom: 1.2rem;">
                <h3 style="font-size: 1rem; font-weight: 700; margin-bottom: 0.8rem; color: var(--text-secondary);">Автопроверяемые задания:</h3>
                ${autoAnswers.map(ans => `
                    <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem; border-bottom: 1px solid var(--border-color); font-size: 0.9rem;">
                        <span><strong>№${ans.itemIndex}</strong> ${ans.subtopic || ''}</span>
                        <span>
                            Ответ: <code>${ans.givenAnswer || '—'}</code>
                            ${ans.isCorrect ? '<span class="badge badge-success" style="margin-left: 0.5rem;">Верно</span>' : '<span class="badge badge-danger" style="margin-left: 0.5rem;">Неверно</span>'}
                        </span>
                    </div>
                `).join('')}
            </div>
        ` : '';

        const modalHtml = `
            <div id="modal-container" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1050; padding: 1.5rem;">
                <div class="card" style="width: 100%; max-width: 820px; max-height: 92vh; overflow-y: auto;">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
                        <div>
                            <h2 style="font-size: 1.3rem; font-weight: 700; margin: 0;">Проверка работы ученика</h2>
                            <p style="color: var(--text-secondary); margin-top: 0.3rem;">Ученик: <strong>${details.studentName}</strong> | Вариант: <strong>${details.variantTitle}</strong></p>
                        </div>
                        <span class="badge badge-info">Текущий балл: ${details.scorePercent !== null ? details.scorePercent + '%' : 'Не выставлен'}</span>
                    </div>

                    ${autoSummaryHtml}
                    ${openInputsHtml}

                    ${openAnswers.length === 0 ? '<p style="color: var(--text-secondary); text-align: center; padding: 1rem;">Все задания автопроверяемые. Ручная оценка не требуется.</p>' : ''}

                    <div style="display: flex; gap: 1rem; justify-content: flex-end; border-top: 1px solid var(--border-color); padding-top: 1rem; margin-top: 0.5rem;">
                        <button type="button" onclick="closeModal()" class="btn btn-secondary">Закрыть</button>
                        ${openAnswers.length > 0 ? `<button type="button" id="btn-save-grades" onclick="savePerTaskGrades(${variantId}, ${attemptId})" class="btn btn-primary">Сохранить оценки</button>` : ''}
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        triggerKaTeX();

        // Clamp input values on change
        document.querySelectorAll('.task-score-input').forEach(input => {
            input.addEventListener('change', () => {
                const max = parseInt(input.getAttribute('data-max-score'), 10) || 1;
                let val = parseInt(input.value, 10) || 0;
                if (val < 0) val = 0;
                if (val > max) val = max;
                input.value = val;
            });
        });

    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function savePerTaskGrades(variantId, attemptId) {
    const inputs = document.querySelectorAll('.task-score-input');
    const taskGrades = [];
    let valid = true;

    inputs.forEach(input => {
        const taskId = parseInt(input.getAttribute('data-task-id'), 10);
        const max = parseInt(input.getAttribute('data-max-score'), 10) || 1;
        let score = parseInt(input.value, 10);
        if (isNaN(score) || score < 0) score = 0;
        if (score > max) { score = max; input.value = max; }
        taskGrades.push({ taskId, score });
    });

    if (!valid || taskGrades.length === 0) {
        showToast('Проверьте введённые баллы', 'error');
        return;
    }

    const btn = document.getElementById('btn-save-grades');
    if (btn) { btn.disabled = true; btn.textContent = 'Сохранение...'; }

    try {
        await apiFetch(`/api/teacher/variants/${variantId}/attempts/${attemptId}/grade`, {
            method: 'POST',
            body: JSON.stringify({ taskGrades })
        });
        closeModal();
        showToast('✅ Оценки успешно сохранены! Итоговый балл пересчитан.');
        renderVariantStatsModalById(variantId);
    } catch (err) {
        if (btn) { btn.disabled = false; btn.textContent = '💾 Сохранить оценки'; }
        showToast('Ошибка при сохранении: ' + err.message, 'error');
    }
}

async function allowStudentRetake(variantId, studentId) {
    try {
        await apiFetch(`/api/teacher/variants/${variantId}/students/${studentId}/allow-retake`, { method: 'POST' });
        showToast('Пересдача разрешена! Ученик может заново пройти тест по той же ссылке (прошлый результат сохранён в истории).');
    } catch (err) {
        showToast(err.message);
    }
}

async function deleteVariant(id) {
    if (!confirm('Удалить этот вариант теста?')) return;
    try {
        await apiFetch(`/api/teacher/variants/${id}`, { method: 'DELETE' });
        loadTeacherTabContent();
    } catch (err) {
        showToast(err.message);
    }
}

// ==========================================
// STUDENT TEST PLAYER VIEW
// ==========================================
async function renderStudentTestView(accessToken) {
    const app = document.getElementById('app');

    try {
        const test = await apiFetch(`/api/public/tests/${accessToken}`);
        state.studentTest = test;

        if (state.studentResult) {
            renderStudentResultScreen();
            return;
        }

        if (state.studentAttempt) {
            renderStudentSolvingScreen();
            return;
        }

        // Start screen
        app.innerHTML = `
            <div style="min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 1.5rem;">
                <div class="card" style="width: 100%; max-width: 500px; text-align: center;">
                    <span class="badge badge-info" style="margin-bottom: 0.5rem;">${test.subject} — ${test.examType}</span>
                    <h1 style="font-size: 1.6rem; font-weight: 700; margin-bottom: 1rem;">${test.title}</h1>
                    <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">
                        Количество заданий: <strong>${test.totalTasks}</strong>
                    </p>

                    <div id="st-error" class="badge badge-danger" style="display:none; margin-bottom: 1rem; width: 100%; padding: 0.8rem;"></div>

                    <form id="start-test-form">
                        <div class="form-group" style="text-align: left;">
                            <label class="form-label">Введите ваши ФИО (строго на русском языке)</label>
                            <input type="text" id="student-name-input" data-mask="cyrillic" class="form-control" placeholder="Иванов Иван" required>
                            <small class="input-hint">Только русские буквы (буквы А-Я, а-я, Ё, ё)</small>
                        </div>
                        <button type="submit" class="btn btn-primary" style="width: 100%; padding: 0.8rem;">
                            Начать прохождение теста
                        </button>
                    </form>
                </div>
            </div>
        `;

        applyInputMasks();

        document.getElementById('start-test-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const studentName = document.getElementById('student-name-input').value.trim();
            const browserFingerprint = getBrowserFingerprint();

            try {
                const attempt = await apiFetch(`/api/public/tests/${accessToken}/start`, {
                    method: 'POST',
                    body: JSON.stringify({ studentName, browserFingerprint })
                });

                state.studentAttempt = attempt;
                state.studentAnswers = {};
                renderStudentSolvingScreen();
            } catch (err) {
                document.getElementById('st-error').textContent = err.message;
                document.getElementById('st-error').style.display = 'block';
            }
        });
    } catch (err) {
        app.innerHTML = `
            <div style="min-height: 100vh; display: flex; justify-content: center; align-items: center;">
                <div class="card" style="text-align: center;">
                    <h2 class="badge badge-danger" style="font-size: 1.2rem; padding: 0.8rem 1.5rem;">Ошибка доступа к тесту</h2>
                    <p style="margin-top: 1rem; color: var(--text-secondary);">${err.message}</p>
                </div>
            </div>
        `;
    }
}

function renderStudentSolvingScreen() {
    const app = document.getElementById('app');
    const test = state.studentTest;
    const attempt = state.studentAttempt;

    app.innerHTML = `
        <nav class="navbar">
            <div class="navbar-brand">⚡ ${test.title}</div>
            <div class="navbar-user">
                <span class="user-badge">Ученик: ${attempt.studentName}</span>
            </div>
        </nav>

        <div class="container">
            <div class="card">
                ${test.tasks.map((task, idx) => `
                    <div class="task-card" id="task-block-${task.id}">
                        <div class="task-header">
                            <span class="task-number">Задание №${idx + 1} <code style="font-size:0.8rem; color:var(--text-secondary); opacity:0.85; margin-left:5px;">[${task.publicId || ('T-' + task.id)}]</code></span>
                            <span class="badge badge-info">${task.subtopic}</span>
                        </div>
                        <div class="task-content">${task.content}</div>

                        ${task.images && task.images.length ? `
                            <div class="task-image-grid">
                                ${task.images.map((img, i) => `
                                    <div class="task-image-item">
                                        ${task.images.length > 1 ? `<span class="image-option-badge">${i + 1})</span>` : ''}
                                        <img src="${img.url}" alt="Diagram ${i + 1}">
                                    </div>
                                `).join('')}
                            </div>
                        ` : ''}

                        <div class="form-group" style="margin-top: 1rem;">
                            <label class="form-label">Ваш ответ:</label>
                            <input type="text" class="form-control" style="max-width: 300px;" 
                                   placeholder="Введите ответ" 
                                   value="${state.studentAnswers[task.id] || ''}"
                                   onchange="saveStudentAnswer(${task.id}, this.value)">
                        </div>
                    </div>
                `).join('')}

                <div style="text-align: center; margin-top: 2rem;">
                    <button onclick="submitStudentTest()" class="btn btn-primary" style="font-size: 1.1rem; padding: 0.8rem 2.5rem;">
                        ✅ Завершить и сдать тест
                    </button>
                </div>
            </div>
        </div>
    `;

    triggerKaTeX();
}

function saveStudentAnswer(taskId, value) {
    state.studentAnswers[taskId] = value;
}

async function submitStudentTest() {
    if (!confirm('Вы уверены, что хотите завершить и отправить тест на проверку?')) return;

    const accessToken = state.studentTest.accessToken;
    const answers = Object.keys(state.studentAnswers).map(taskId => ({
        taskId: parseInt(taskId, 10),
        answer: state.studentAnswers[taskId]
    }));

    try {
        const result = await apiFetch(`/api/public/tests/${accessToken}/submit`, {
            method: 'POST',
            body: JSON.stringify({
                attemptId: state.studentAttempt.attemptId,
                answers
            })
        });

        state.studentResult = result;
        localStorage.setItem('completed_test_' + accessToken, JSON.stringify({ completedAt: new Date().toISOString() }));
        renderStudentResultScreen();
    } catch (err) {
        showToast('Ошибка отправки: ' + err.message);
    }
}

function renderStudentResultScreen() {
    const app = document.getElementById('app');
    const res = state.studentResult;

    app.innerHTML = `
        <div class="container" style="max-width: 800px; margin-top: 3rem;">
            <div class="card score-hero">
                <h1 style="margin-bottom: 0.5rem;">Тест завершен!</h1>
                <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">Результаты ученика: <strong>${res.studentName}</strong></p>

                ${res.scorePercent !== null ? `
                    <div class="score-circle">
                        ${res.scorePercent}%
                    </div>
                    <p style="font-size: 1.1rem;">
                        Правильно решено <strong>${res.correctCount}</strong> из <strong>${res.gradableTasks}</strong> проверяемых заданий.
                    </p>
                ` : `
                    <div class="score-circle" style="font-size: 1.2rem;">
                        Успешно
                    </div>
                    <p style="font-size: 1.1rem;">Тест сдан. Задания в данном тесте требуют проверки преподавателем.</p>
                `}
            </div>

            <div class="card">
                <h3 style="margin-bottom: 1rem;">Подробный разбор заданий</h3>

                ${res.feedback.map(f => `
                    <div class="task-card" style="border-left: 4px solid ${
                        f.status === 'CORRECT' ? 'var(--success)' : (f.status === 'INCORRECT' ? 'var(--danger)' : 'var(--warning)')
                    }">
                        <div class="task-header">
                            <div>
                                <span class="task-number">Задание №${f.itemIndex}</span> (${f.subtopic})
                            </div>
                            <div>
                                ${f.status === 'CORRECT' ? '<span class="badge badge-success">Верно</span>' : ''}
                                ${f.status === 'INCORRECT' ? '<span class="badge badge-danger">Неверно</span>' : ''}
                                ${f.status === 'UNGRADED' ? '<span class="badge badge-warning">Проверяется преподавателем</span>' : ''}
                            </div>
                        </div>
                        <div style="margin-top: 0.5rem; font-size: 0.95rem;">
                            Ваш ответ: <strong>${f.givenAnswer || '—'}</strong>
                            ${f.correctAnswer ? ` | Правильный ответ: <strong style="color: var(--success);">${f.correctAnswer}</strong>` : ''}
                        </div>
                    </div>
                `).join('')}
            </div>
        </div>
    `;

    triggerKaTeX();
}

function closeModal() {
    const modal = document.getElementById('modal-container');
    if (modal) modal.remove();
}

// ==========================================
// SPA ROUTER & INITIALIZATION
// ==========================================
function handleRoute() {
    const hash = window.location.hash || '';

    if (hash.startsWith('#test/')) {
        const accessToken = hash.replace('#test/', '');
        renderStudentTestView(accessToken);
    } else if (hash === '#admin') {
        if (!state.token || !state.user || state.user.role !== 'ADMIN') {
            window.location.hash = '#login';
            renderLoginView();
        } else {
            renderAdminDashboard();
        }
    } else if (hash === '#teacher') {
        if (!state.token || !state.user) {
            window.location.hash = '#login';
            renderLoginView();
        } else {
            renderTeacherDashboard();
        }
    } else {
        if (state.token && state.user) {
            if (state.user.role === 'ADMIN') {
                window.location.hash = '#admin';
                renderAdminDashboard();
            } else {
                window.location.hash = '#teacher';
                renderTeacherDashboard();
            }
        } else {
            renderLoginView();
        }
    }
}

window.addEventListener('hashchange', handleRoute);
window.addEventListener('DOMContentLoaded', handleRoute);

// Execute immediately if DOM is already parsed
if (document.readyState === 'complete' || document.readyState === 'interactive') {
    handleRoute();
}
