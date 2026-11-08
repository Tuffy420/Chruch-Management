/**
 * GraceConnect Church Family & Events Management Dashboard Logic
 * Core Javascript - Unified REST API client connected to Spring Boot
 */

document.addEventListener('DOMContentLoaded', () => {
    // ==========================================================================
    // 1. DATA STATE & CONFIGURATION
    // ==========================================================================
    const API_BASE = window.__API_BASE__ || 
                     localStorage.getItem('gc_api_base') || 
                     (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' 
                        ? 'http://localhost:8080/api' 
                        : 'https://church-management-backend-hwtc.onrender.com/api');

    let state = {
        token: localStorage.getItem('gc_token') || null,
        user: JSON.parse(localStorage.getItem('gc_user')) || null,
        families: [],
        members: [],
        events: [],
        transactions: [],
        dashboardData: null
    };

    // DOM Elements
    const loginScreen = document.getElementById('loginScreen');
    const appContainer = document.querySelector('.app-container');
    const loginForm = document.getElementById('loginForm');
    const welcomeTitle = document.getElementById('welcomeTitle');
    const profileName = document.querySelector('.profile-name');
    const profileRole = document.querySelector('.profile-role');
    const profileAvatar = document.querySelector('.profile-avatar img');

    // ==========================================================================
    // 2. AUTHENTICATION & REST WRAPPERS
    // ==========================================================================

    function checkAuth() {
        if (state.token) {
            loginScreen.style.display = 'none';
            appContainer.style.display = 'grid';

            // Set profile text
            if (state.user) {
                welcomeTitle.textContent = `Welcome, ${state.user.fullName}`;
                profileName.textContent = state.user.fullName;
                profileRole.textContent = state.user.role;
                profileAvatar.src = `https://placehold.co/100/3b82f6/ffffff?text=${state.user.fullName[0]}`;
            }

            // Initial Dashboard Load
            switchPage('dashboard');
        } else {
            loginScreen.style.display = 'flex';
            appContainer.style.display = 'none';
        }
    }

    // Standard headers for JWT-secured endpoints
    function getHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${state.token}`
        };
    }

    // Handles API requests with auto-logout on 401 Unauthorized
    async function apiRequest(endpoint, options = {}) {
        options.headers = getHeaders();
        try {
            const response = await fetch(`${API_BASE}${endpoint}`, options);
            if (response.status === 401 || response.status === 403) {
                logout();
                throw new Error('Session expired, please login again.');
            }
            const json = await response.json();
            if (!response.ok) {
                throw new Error(json.message || 'Something went wrong');
            }
            return json;
        } catch (error) {
            showToast('API Error', error.message, 'warning');
            throw error;
        }
    }

    function logout() {
        state.token = null;
        state.user = null;
        localStorage.removeItem('gc_token');
        localStorage.removeItem('gc_user');
        showToast('Signed Out', 'You have been securely signed out.', 'info');
        checkAuth();
    }

    // Bind Profile Click to Logout Trigger
    const profileBtn = document.getElementById('profileBtn');
    if (profileBtn) {
        profileBtn.addEventListener('click', () => {
            if (confirm('Do you want to log out?')) {
                logout();
            }
        });
    }

    // Handle Login Submit
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('loginEmail').value;
            const password = document.getElementById('loginPassword').value;

            try {
                const response = await fetch(`${API_BASE}/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const json = await response.json();

                if (!response.ok) {
                    throw new Error(json.message || 'Invalid credentials');
                }

                state.token = json.data.token;
                state.user = {
                    email: json.data.email,
                    role: json.data.role,
                    fullName: json.data.fullName
                };

                localStorage.setItem('gc_token', state.token);
                localStorage.setItem('gc_user', JSON.stringify(state.user));

                showToast('Login Successful', `Welcome back, ${state.user.fullName}!`);
                checkAuth();
            } catch (error) {
                showToast('Login Failed', error.message, 'warning');
            }
        });
    }

    // ==========================================================================
    // 3. TIMERS, CLOCK & SCRIPTURE CYCLER
    // ==========================================================================

    function updateClock() {
        const dateElement = document.getElementById('navbarDate');
        if (dateElement) {
            const now = new Date();
            const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            const timeOptions = { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true };
            dateElement.textContent = `${now.toLocaleDateString('en-US', options)} • ${now.toLocaleTimeString('en-US', timeOptions)}`;
        }
    }
    setInterval(updateClock, 1000);
    updateClock();

    // Scriptures Baseline Cycle
    const scriptures = [
        { text: '“Let all that you do be done in love.”', ref: '1 Corinthians 16:14' },
        { text: '“The Lord is my shepherd; I shall not want.”', ref: 'Psalm 23:1' },
        { text: '“Above all, keep loving one another earnestly, since love covers a multitude of sins.”', ref: '1 Peter 4:8' },
        { text: '“For where two or three are gathered in my name, there am I among them.”', ref: 'Matthew 18:20' },
        { text: '“I can do all things through Him who strengthens me.”', ref: 'Philippians 4:13' },
        { text: '“Be kind to one another, tenderhearted, forgiving one another, as God in Christ forgave you.”', ref: 'Ephesians 4:32' }
    ];
    let scriptureIndex = 0;
    function rotateScripture() {
        const textElem = document.getElementById('bibleText');
        const refElem = document.getElementById('bibleRef');
        if (textElem && refElem) {
            textElem.style.opacity = 0;
            refElem.style.opacity = 0;
            setTimeout(() => {
                scriptureIndex = (scriptureIndex + 1) % scriptures.length;
                const nextScripture = scriptures[scriptureIndex];
                textElem.textContent = nextScripture.text;
                refElem.textContent = nextScripture.ref;
                textElem.style.opacity = 1;
                refElem.style.opacity = 1;
            }, 300);
        }
    }
    setInterval(rotateScripture, 15000);

    // ==========================================================================
    // 4. ROUTING & SWITCHING VIEWS
    // ==========================================================================

    let currentActivePage = 'dashboard';
    let eventViewMode = 'calendar'; // 'calendar' or 'list'
    let eventsCalendarDate = new Date(2026, 4, 25); // Set to May 2026 baseline

    const menuItems = document.querySelectorAll('.sidebar-menu .menu-item');
    const dashboardView = document.getElementById('dashboardView');
    const subpageViews = document.querySelectorAll('.subpage-view');

    function switchPage(pageId) {
        currentActivePage = pageId;

        // Toggle Sidebar Active styling
        menuItems.forEach(item => {
            if (item.getAttribute('data-page') === pageId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Toggle Main Dash Content or Dynamic Renders
        if (pageId === 'dashboard') {
            dashboardView.style.display = 'block';
            subpageViews.forEach(view => view.style.display = 'none');
            fetchDashboardData();
        } else {
            dashboardView.style.display = 'none';
            subpageViews.forEach(view => {
                // Handle marriage exception path
                const matchesView = (view.id === `${pageId}View` || (pageId === 'marriage-anniversaries' && view.id === 'marriageView'));
                view.style.display = matchesView ? 'block' : 'none';
            });

            // Get current active search query if any
            const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';

            // Trigger dynamic fetch on subpage switch
            if (pageId === 'families') fetchFamiliesTab(searchVal);
            if (pageId === 'events') fetchEventsTab(searchVal);
            if (pageId === 'accounts') fetchAccountsTab(searchVal);
            if (pageId === 'birthdays') fetchCelebrationRemindersTab('birthdays', searchVal);
            if (pageId === 'marriage-anniversaries') fetchCelebrationRemindersTab('anniversaries', searchVal);
            if (pageId === 'reminders') renderRemindersTab();
            if (pageId === 'reports') renderReportsTab();
            if (pageId === 'settings') renderSettingsTab();
        }
    }

    menuItems.forEach(item => {
        item.addEventListener('click', () => {
            const pageId = item.getAttribute('data-page');
            const searchInput = document.getElementById('searchInput');
            if (searchInput) searchInput.value = ''; // Reset search on manual sidebar click
            switchPage(pageId);
        });
    });

    // Debounced Global Search Bar Event Handler
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        let debounceTimeout;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimeout);
            debounceTimeout = setTimeout(() => {
                const query = searchInput.value.trim();
                
                // If on dashboard, seamlessly redirect to families directory with active search
                if (currentActivePage === 'dashboard') {
                    switchPage('families');
                } else {
                    // Trigger active page refresh with active search query
                    if (currentActivePage === 'families') fetchFamiliesTab(query);
                    if (currentActivePage === 'events') fetchEventsTab(query);
                    if (currentActivePage === 'accounts') fetchAccountsTab(query);
                    if (currentActivePage === 'birthdays') fetchCelebrationRemindersTab('birthdays', query);
                    if (currentActivePage === 'marriage-anniversaries') fetchCelebrationRemindersTab('anniversaries', query);
                }
            }, 300);
        });
    }

    // Events View Toggles (Calendar vs List view)
    const eventViewCalendarBtn = document.getElementById('eventViewCalendarBtn');
    const eventViewListBtn = document.getElementById('eventViewListBtn');
    const eventsListContainer = document.getElementById('eventsListContainer');
    const eventsCalendarContainer = document.getElementById('eventsCalendarContainer');

    function setEventViewMode(mode) {
        eventViewMode = mode;
        if (mode === 'calendar') {
            if (eventViewCalendarBtn) eventViewCalendarBtn.classList.add('active');
            if (eventViewListBtn) eventViewListBtn.classList.remove('active');
            if (eventsListContainer) eventsListContainer.style.display = 'none';
            if (eventsCalendarContainer) eventsCalendarContainer.style.display = 'block';
        } else {
            if (eventViewCalendarBtn) eventViewCalendarBtn.classList.remove('active');
            if (eventViewListBtn) eventViewListBtn.classList.add('active');
            if (eventsListContainer) eventsListContainer.style.display = 'grid';
            if (eventsCalendarContainer) eventsCalendarContainer.style.display = 'none';
        }
        const query = searchInput ? searchInput.value.trim() : '';
        fetchEventsTab(query);
    }

    if (eventViewCalendarBtn) {
        eventViewCalendarBtn.addEventListener('click', () => setEventViewMode('calendar'));
    }
    if (eventViewListBtn) {
        eventViewListBtn.addEventListener('click', () => setEventViewMode('list'));
    }

    // Populate dynamic dropdown selectors inside modals
    async function refreshDropdowns() {
        try {
            const famRes = await apiRequest('/families?size=200');
            const familySelect = document.getElementById('memberFamilyRef');
            if (familySelect && famRes.data && famRes.data.content) {
                familySelect.innerHTML = famRes.data.content.map(fam => `<option value="${fam.id}">${fam.name}</option>`).join('');
            }

            const memRes = await apiRequest('/members?size=200');
            const memberSelect = document.getElementById('donationMember');
            if (memberSelect && memRes.data && memRes.data.content) {
                const options = [
                    '<option value="General Sunday Congregation">General Sunday Congregation (Basket Collection)</option>',
                    ...memRes.data.content.map(mem => `<option value="${mem.firstName} ${mem.lastName}">${mem.firstName} ${mem.lastName}</option>`),
                    '<option value="Anonymous / Other Giver">Anonymous / Other Giver</option>'
                ];
                memberSelect.innerHTML = options.join('');
            }
        } catch (e) {
            console.error('Dropdown refresh error', e);
        }
    }


    // ==========================================================================
    // 5. REST FETCH AND PAGE RENDER LAUNCHERS
    // ==========================================================================

    // Fetch Dashboard Stats, Charts, and Timelines
    async function fetchDashboardData() {
        try {
            const json = await apiRequest('/dashboard/summary');
            state.dashboardData = json.data;

            // Render numeric metrics
            document.getElementById('countFamilies').textContent = state.dashboardData.totalFamilies;
            document.getElementById('countMembers').textContent = state.dashboardData.totalMembers;
            document.getElementById('countBirthdays').textContent = state.dashboardData.upcomingBirthdaysCount;
            document.getElementById('countFunctions').textContent = state.dashboardData.upcomingEventsCount;
            document.getElementById('treasuryBalance').textContent = `₹${state.dashboardData.treasuryBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

            const countTreasuryEl = document.getElementById('countTreasury');
            if (countTreasuryEl) {
                countTreasuryEl.textContent = `₹${state.dashboardData.treasuryBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
            }

            // Render dynamic Monthly Donations
            const countDonationsEl = document.getElementById('countDonations');
            if (countDonationsEl) {
                countDonationsEl.textContent = `₹${state.dashboardData.monthlyDonations.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
            }

            // Render dynamic Monthly Donations Trend
            const trendEl = document.querySelector('.metric-card.purple .metric-trend');
            if (trendEl) {
                const trend = state.dashboardData.monthlyDonationsTrend;
                const sign = trend >= 0 ? '+' : '';
                const dirClass = trend >= 0 ? 'up' : 'down';
                const iconClass = trend >= 0 ? 'ri-arrow-up-line' : 'ri-arrow-down-line';

                trendEl.className = `metric-trend ${dirClass}`;
                trendEl.innerHTML = `<i class="${iconClass}"></i> ${sign}${trend.toFixed(1)}%`;
            }

            // Render dynamic fund breakdown balances
            if (state.dashboardData.fundBalances) {
                const fb = state.dashboardData.fundBalances;
                const updateFund = (id, key) => {
                    const el = document.getElementById(id);
                    if (el) {
                        const val = fb[key] || 0.0;
                        el.textContent = `₹${val.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
                    }
                };
                updateFund('balanceWorship', 'Sunday Worship Offering');
                updateFund('balanceGeneral', 'General Fund');
                updateFund('balanceMissions', 'Missions & Outreach');
                updateFund('balanceMaintenance', 'Building Maintenance');
                updateFund('balanceCharity', 'Charity & Welfare');
            }

            // Dynamic Chart drawing
            renderDashboardChart(
                state.dashboardData.chartMonths,
                state.dashboardData.incomeChartData,
                state.dashboardData.expenseChartData
            );

            // Populate timeline
            const timelineContainer = document.getElementById('timelineContainer');
            if (timelineContainer && state.dashboardData.recentActivities) {
                timelineContainer.innerHTML = state.dashboardData.recentActivities.map(act => {
                    let styleClass = 'purple';
                    let icon = 'ri-user-add-line';
                    if (act.module === 'Member') { styleClass = 'blue'; icon = 'ri-user-heart-line'; }
                    if (act.module === 'Offering') { styleClass = 'teal'; icon = 'ri-wallet-3-line'; }
                    if (act.module === 'Schedule') { styleClass = 'rose'; icon = 'ri-calendar-event-line'; }

                    const timeStr = new Date(act.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                    return `
                        <div class="timeline-item ${styleClass}">
                            <div class="timeline-dot"></div>
                            <div class="timeline-content">
                                <div class="timeline-info">
                                    <span class="timeline-text">${act.action}</span>
                                    <div class="timeline-meta">
                                        <i class="${icon}"></i> ${act.performedBy} <span></span> ${timeStr}
                                    </div>
                                </div>
                                <span class="timeline-badge ${styleClass}">${act.module}</span>
                            </div>
                        </div>
                    `;
                }).join('');
            }

            // Populate upcoming services on the right sidebar widget
            fetchUpcomingServicesWidget();

        } catch (e) {
            console.error('Dashboard data fetch error', e);
        }
    }

    // Fetch upcoming services for sidebar widget
    async function fetchUpcomingServicesWidget() {
        const widgetContainer = document.getElementById('upcomingFunctionsWidgetList');
        if (!widgetContainer) return;
        try {
            const json = await apiRequest('/events/upcoming');
            if (json.data && json.data.length > 0) {
                widgetContainer.innerHTML = json.data.slice(0, 3).map(ev => {
                    const d = new Date(ev.date);
                    const day = d.getDate();
                    const month = d.toLocaleDateString('en-US', { month: 'short' });
                    const catClass = ev.category.toLowerCase();
                    return `
                        <div class="function-item">
                            <div class="function-date-badge">
                                <span class="day">${day}</span>
                                <span class="month">${month}</span>
                            </div>
                            <div class="function-info">
                                <span class="function-title">${ev.title}</span>
                                <div class="function-time-loc">
                                    <span><i class="ri-time-line"></i> ${ev.time || 'Worship'}</span>
                                    <span><i class="ri-map-pin-line"></i> ${ev.location}</span>
                                </div>
                                <span class="function-category ${catClass}">${ev.category}</span>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        } catch (e) {
            console.error('Sidebar functions load error', e);
        }
    }

    // Fetch Families tab
    async function fetchFamiliesTab(search = '') {
        const container = document.getElementById('familiesListContainer');
        if (!container) return;
        try {
            const json = await apiRequest(`/families?size=200&sortBy=name&sortDir=asc&search=${encodeURIComponent(search)}`);
            state.families = json.data.content;
            
            if (state.families.length === 0) {
                container.innerHTML = `
                    <div style="padding: 40px 24px; text-align: center; color: var(--text-light); width: 100%;">
                        <i class="ri-search-eye-line" style="font-size: 38px; color: var(--primary-purple); display: block; margin-bottom: 12px; opacity: 0.7;"></i>
                        <p style="font-size: 14px; font-weight: 500;">No family households matched your search query.</p>
                        <p style="font-size: 12px; margin-top: 4px; color: var(--text-light);">Try searching for another household last name or individual member name.</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = state.families.map(fam => {
                const count = fam.members ? fam.members.length : 0;
                
                const membersListHtml = count > 0 
                    ? `<div class="family-members-collapse" id="fam-members-${fam.id}" style="display: none; margin-top: 12px; padding-top: 12px; border-top: 1px dashed rgba(0,0,0,0.08); flex-direction: column; gap: 8px;">
                        ${fam.members.map(m => `
                            <div class="family-member-item" style="display: flex; align-items: center; gap: 10px; padding: 6px 8px; background: rgba(255,255,255,0.5); border-radius: 8px; border: 1px solid rgba(0,0,0,0.03);">
                                <img src="${m.profilePhoto}" alt="${m.firstName}" onerror="this.src='https://placehold.co/100/8b5cf6/ffffff?text=${m.firstName[0]}'" style="width: 28px; height: 28px; border-radius: 50%; object-fit: cover;">
                                <div style="display: flex; flex-direction: column; flex-grow: 1;">
                                    <span style="font-size: 13px; font-weight: 600; color: var(--text-main);">${m.firstName} ${m.lastName}</span>
                                    <span style="font-size: 10px; color: var(--text-light);">${m.gender} • DOB: ${m.dob} • ${m.baptismStatus}</span>
                                </div>
                                <button class="subpage-btn" style="padding: 2px 6px; font-size: 10px; background: rgba(244, 63, 94, 0.05); color: var(--primary-rose); border: 1px solid rgba(244, 63, 94, 0.1);" onclick="event.stopPropagation(); removeMemberFromFamily('${m.id}', '${fam.id}')"><i class="ri-user-minus-line"></i> Remove</button>
                            </div>
                        `).join('')}
                       </div>`
                    : `<p style="font-size: 11px; color: var(--text-light); margin-top: 8px; font-style: italic;">No members registered yet.</p>`;

                return `
                    <div class="records-card">
                        <div class="record-avatar" style="background: var(--soft-blue-gradient); display: flex; align-items: center; justify-content: center; font-size: 20px; color: var(--primary-blue)">
                            <i class="ri-home-4-line"></i>
                        </div>
                        <div class="record-info">
                            <span class="record-title">${fam.name}</span>
                            <span class="record-subtitle"><i class="ri-map-pin-line"></i> ${fam.address || 'Springfield'}</span>
                            <div class="record-subtitle" style="margin-top: 4px;">
                                <span><i class="ri-mail-line"></i> ${fam.email || 'N/A'}</span> • <span><i class="ri-phone-line"></i> ${fam.phone || 'N/A'}</span>
                            </div>
                            <span class="record-tag">${count} Registered Members</span>
                            <div style="margin-top:12px; display:flex; gap:8px;">
                                <button class="subpage-btn" style="padding:4px 8px; font-size:11px;" onclick="toggleMembersCollapse('${fam.id}')"><i class="ri-group-line"></i> ${count > 0 ? 'View Members' : 'No Members'}</button>
                                <button class="subpage-btn" style="padding:4px 8px; font-size:11px;" onclick="openAddMemberModal('${fam.id}')"><i class="ri-user-add-line"></i> Add Member</button>
                                <button class="subpage-btn" style="padding:4px 8px; font-size:11px;" onclick="deleteFamilyRecord('${fam.id}')"><i class="ri-delete-bin-line"></i> Delete</button>
                            </div>
                            ${membersListHtml}
                        </div>
                    </div>
                `;
            }).join('');
        } catch (e) {
            container.innerHTML = `<p style="padding: 24px; color: var(--primary-rose);">Unable to load families. Check database connection.</p>`;
        }
    }

    // Global helpers for family card operations
    window.toggleMembersCollapse = function (famId) {
        const el = document.getElementById(`fam-members-${famId}`);
        if (el) {
            el.style.display = el.style.display === 'none' ? 'flex' : 'none';
        }
    };

    window.openAddMemberModal = function (familyId) {
        const familyRefSelect = document.getElementById('memberFamilyRef');
        if (familyRefSelect) {
            refreshDropdowns().then(() => {
                familyRefSelect.value = familyId;
                openModal('addMember');
            });
        } else {
            openModal('addMember');
        }
    };

    window.removeMemberFromFamily = async function (memberId, familyId) {
        if (confirm('Are you sure you want to remove this member from the household roster?')) {
            try {
                await apiRequest(`/members/${memberId}`, { method: 'DELETE' });
                showToast('Member Removed', 'Successfully removed member from family household.');
                
                const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';
                fetchFamiliesTab(searchVal);
            } catch (e) { }
        }
    };

    // Dynamic record deletes
    window.deleteFamilyRecord = async function (id) {
        if (confirm('Are you sure you want to delete this family household? This will cascade delete linked members!')) {
            try {
                await apiRequest(`/families/${id}`, { method: 'DELETE' });
                showToast('Deleted', 'Family deleted successfully.');
                
                const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';
                fetchFamiliesTab(searchVal);
            } catch (e) { }
        }
    };

    // Fetch Events tab
    async function fetchEventsTab(search = '') {
        const container = document.getElementById('eventsListContainer');
        const calContainer = document.getElementById('eventsCalendarContainer');
        if (!container || !calContainer) return;

        try {
            const json = await apiRequest('/events?size=200&sortBy=date&sortDir=asc');
            let events = json.data.content;
            
            // Filter events locally by the search string
            if (search.trim() !== '') {
                const term = search.toLowerCase().trim();
                events = events.filter(ev => 
                    ev.title.toLowerCase().includes(term) ||
                    ev.category.toLowerCase().includes(term) ||
                    (ev.location && ev.location.toLowerCase().includes(term)) ||
                    (ev.time && ev.time.toLowerCase().includes(term))
                );
            }

            state.events = events;

            // List view rendering
            if (events.length === 0) {
                container.innerHTML = `
                    <div style="padding: 40px 24px; text-align: center; color: var(--text-light); width: 100%;">
                        <i class="ri-search-eye-line" style="font-size: 38px; color: var(--primary-purple); display: block; margin-bottom: 12px; opacity: 0.7;"></i>
                        <p style="font-size: 14px; font-weight: 500;">No scheduled events matched your search filter.</p>
                    </div>
                `;
            } else {
                container.innerHTML = events.map(ev => `
                    <div class="records-card">
                        <div class="record-avatar" style="background: rgba(139, 92, 246, 0.08); display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--primary-purple)">
                            <i class="ri-calendar-event-line"></i>
                        </div>
                        <div class="record-info">
                            <span class="record-title">${ev.title}</span>
                            <span class="record-subtitle"><i class="ri-time-line"></i> ${ev.time} • <i class="ri-map-pin-line"></i> ${ev.location}</span>
                            <div class="record-subtitle" style="margin-top: 4px; font-weight: 600; color: var(--primary-purple)">
                                Date: ${new Date(ev.date).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
                            </div>
                            <span class="record-tag" style="text-transform: uppercase;">${ev.category}</span>
                            <div style="margin-top:12px; display:flex; gap:8px;">
                                <button class="subpage-btn" style="padding:4px 8px; font-size:11px;" onclick="deleteEventRecord('${ev.id}')"><i class="ri-delete-bin-line"></i> Cancel</button>
                            </div>
                        </div>
                    </div>
                `).join('');
            }

            // Calendar view rendering
            renderFullScreenEventsCalendar();

        } catch (e) {
            container.innerHTML = `<p style="padding: 24px; color: var(--primary-rose);">Unable to load event schedules.</p>`;
        }
    }

    // Gorgeous full screen monthly calendar grid rendering logic
    function renderFullScreenEventsCalendar() {
        const calContainer = document.getElementById('eventsCalendarContainer');
        if (!calContainer) return;

        const year = eventsCalendarDate.getFullYear();
        const month = eventsCalendarDate.getMonth();
        const monthsList = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];

        const firstDayIndex = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const daysInPrevMonth = new Date(year, month, 0).getDate();

        calContainer.innerHTML = `
            <div class="events-calendar-view">
                <div class="calendar-controls">
                    <div class="calendar-title-box">
                        <button class="subpage-btn" id="calTodayBtn" style="padding: 6px 12px; font-weight: 600;">Today</button>
                        <div style="display: flex; gap: 8px;">
                            <button class="calendar-nav-btn" id="calPrevBtn"><i class="ri-arrow-left-s-line"></i></button>
                            <button class="calendar-nav-btn" id="calNextBtn"><i class="ri-arrow-right-s-line"></i></button>
                        </div>
                        <h3 style="font-family: 'Outfit'; font-weight: 700; font-size: 20px; color: var(--text-dark); margin: 0;">${monthsList[month]} ${year}</h3>
                    </div>
                </div>
                <div class="calendar-grid-header">
                    <div>Sunday</div><div>Monday</div><div>Tuesday</div><div>Wednesday</div><div>Thursday</div><div>Friday</div><div>Saturday</div>
                </div>
                <div class="calendar-grid-body" id="calGridBody"></div>
            </div>
        `;

        // Bind calendar controls
        document.getElementById('calTodayBtn').addEventListener('click', () => {
            eventsCalendarDate = new Date(2026, 4, 25); // Today in our preseeded system (May 25, 2026)
            renderFullScreenEventsCalendar();
        });
        document.getElementById('calPrevBtn').addEventListener('click', () => {
            eventsCalendarDate.setMonth(eventsCalendarDate.getMonth() - 1);
            renderFullScreenEventsCalendar();
        });
        document.getElementById('calNextBtn').addEventListener('click', () => {
            eventsCalendarDate.setMonth(eventsCalendarDate.getMonth() + 1);
            renderFullScreenEventsCalendar();
        });

        const gridBody = document.getElementById('calGridBody');
        gridBody.innerHTML = '';

        // Render previous month overlap cells
        for (let x = firstDayIndex; x > 0; x--) {
            const cell = document.createElement('div');
            cell.classList.add('calendar-grid-cell', 'other-month');
            const dayNum = daysInPrevMonth - x + 1;
            cell.innerHTML = `<span class="cell-number">${dayNum}</span>`;
            gridBody.appendChild(cell);
        }

        // Render active month cells
        for (let i = 1; i <= daysInMonth; i++) {
            const cell = document.createElement('div');
            cell.classList.add('calendar-grid-cell');
            if (i === 25 && month === 4 && year === 2026) {
                cell.classList.add('today');
            }
            cell.innerHTML = `<span class="cell-number">${i}</span>`;

            // Query pre-filtered state.events for this day
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
            const dayEvents = state.events.filter(ev => ev.date === dateStr);

            dayEvents.forEach(ev => {
                const eventItem = document.createElement('div');
                const catClass = ev.category.toLowerCase();
                eventItem.className = `calendar-event-item ${catClass}`;
                eventItem.title = `${ev.time || 'Worship'} - ${ev.title} @ ${ev.location}`;
                eventItem.innerHTML = `<span style="font-weight: 800;">${ev.time || ''}</span> ${ev.title}`;
                
                eventItem.addEventListener('click', (e) => {
                    e.stopPropagation();
                    showToast(ev.title, `Time: ${ev.time} | Location: ${ev.location} (${ev.category})`, 'info');
                });
                
                cell.appendChild(eventItem);
            });

            gridBody.appendChild(cell);
        }

        // Render next month overlap cells to complete grid structure (6 rows = 42 cells)
        const totalCells = gridBody.children.length;
        const fillCellsNeeded = 42 - totalCells;
        for (let z = 1; z <= fillCellsNeeded; z++) {
            const cell = document.createElement('div');
            cell.classList.add('calendar-grid-cell', 'other-month');
            cell.innerHTML = `<span class="cell-number">${z}</span>`;
            gridBody.appendChild(cell);
        }
    }

    window.deleteEventRecord = async function (id) {
        if (confirm('Are you sure you want to cancel and delete this scheduled event?')) {
            try {
                await apiRequest(`/events/${id}`, { method: 'DELETE' });
                showToast('Deleted', 'Event deleted from calendar.');
                
                const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';
                fetchEventsTab(searchVal);
                renderCalendar(); // redraw calendar highlight dots in sidebar widget
            } catch (e) { }
        }
    };

    // ==========================================================================
    // CHURCH FINANCIAL LEDGER & OFFERINGS / EXPENSES ENGINE
    // ==========================================================================

    const OFFERING_CATEGORIES = [
        'Sunday Worship Offering',
        'Tithe (10%)',
        'Thanksgiving Offering',
        'Youth & Sunday School Offering',
        'Building & Sanctuary Fund',
        'Missions & Outreach Offering',
        'Harvest Festival Offering',
        'Charity & Welfare Fund'
    ];

    const EXPENSE_CATEGORIES = [
        'Electricity & Utility Bills',
        'Sanctuary Maintenance & Repairs',
        'Pastor Honorarium & Staff Allowance',
        'Guest Preacher Honorarium',
        'Worship Sound, Audio & Media Equipment',
        'Fellowship Refreshments & Agape Meal',
        'Poor Relief & Benevolence Aid',
        'Sunday School Books, Bibles & Literature',
        'General Church Operating Expense'
    ];

    let currentFinancialFilter = 'all'; // all, INCOME, EXPENSE

    // Fetch Accounts ledger with live KPI aggregation and filtering
    async function fetchAccountsTab(search = '') {
        const container = document.getElementById('accountsListContainer');
        if (!container) return;
        try {
            const json = await apiRequest('/accounts?size=300&sortBy=date&sortDir=desc');
            let transactions = json.data.content;
            state.transactions = transactions;

            // Compute ledger summary aggregates
            let totalIncome = 0;
            let totalExpense = 0;
            transactions.forEach(t => {
                if (t.type === 'INCOME') totalIncome += (t.amount || 0);
                else if (t.type === 'EXPENSE') totalExpense += (t.amount || 0);
            });

            const incomeEl = document.getElementById('ledgerTotalIncome');
            const expenseEl = document.getElementById('ledgerTotalExpense');
            const balanceEl = document.getElementById('ledgerNetBalance');
            if (incomeEl) incomeEl.textContent = `₹${totalIncome.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
            if (expenseEl) expenseEl.textContent = `₹${totalExpense.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
            if (balanceEl) {
                const net = totalIncome - totalExpense;
                balanceEl.textContent = `₹${net.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
                balanceEl.style.color = net >= 0 ? 'var(--primary-teal)' : 'var(--primary-rose)';
            }

            // Apply financial type filter
            if (currentFinancialFilter !== 'all') {
                transactions = transactions.filter(t => t.type === currentFinancialFilter);
            }

            // Apply text search filter
            if (search.trim() !== '') {
                const term = search.toLowerCase().trim();
                transactions = transactions.filter(t => 
                    (t.memberName && t.memberName.toLowerCase().includes(term)) ||
                    (t.fund && t.fund.toLowerCase().includes(term)) ||
                    (t.type && t.type.toLowerCase().includes(term)) ||
                    (t.notes && t.notes.toLowerCase().includes(term))
                );
            }

            if (transactions.length === 0) {
                container.innerHTML = `
                    <div style="padding: 40px 24px; text-align: center; color: var(--text-light); width: 100%;">
                        <i class="ri-search-eye-line" style="font-size: 38px; color: var(--primary-purple); display: block; margin-bottom: 12px; opacity: 0.7;"></i>
                        <p style="font-size: 14px; font-weight: 500;">No transactions found matching your criteria.</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = transactions.map(t => {
                const isIncome = t.type === 'INCOME';
                const color = isIncome ? 'var(--primary-teal)' : 'var(--primary-rose)';
                const bg = isIncome ? 'rgba(13, 148, 136, 0.08)' : 'rgba(244, 63, 94, 0.08)';
                const symbol = isIncome ? '+' : '-';
                const icon = isIncome ? 'ri-hand-coin-line' : 'ri-shopping-cart-2-line';
                return `
                    <div class="records-card">
                        <div class="record-avatar" style="background: ${bg}; display: flex; align-items: center; justify-content: center; font-size: 22px; color: ${color}">
                            <i class="${icon}"></i>
                        </div>
                        <div class="record-info" style="flex:1;">
                            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                                <span class="record-title">${t.memberName || (isIncome ? 'Sunday Congregation' : 'Church Operational Vendor')}</span>
                                <span class="record-tag" style="background: ${bg}; color: ${color}; font-weight: 700; font-size: 12px; padding: 4px 10px;">
                                    ${symbol}₹${t.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                </span>
                            </div>
                            <span class="record-subtitle" style="font-weight:600; color:var(--text-main);"><i class="ri-wallet-3-line"></i> ${t.fund}</span>
                            <span class="record-subtitle" style="display:block; margin-top:2px;">Memo: ${t.notes || 'No description'} • Date: ${t.date}</span>
                            <div style="margin-top:10px; display:flex; gap:8px;">
                                <button class="subpage-btn" style="padding:3px 8px; font-size:11px;" onclick="deleteTransactionRecord('${t.id}')">
                                    <i class="ri-delete-bin-line"></i> Delete
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        } catch (e) {
            container.innerHTML = `<p style="padding: 24px; color: var(--primary-rose);">Unable to load transactions.</p>`;
        }
    }

    window.deleteTransactionRecord = async function (id) {
        if (!state.token) {
            showToast('Auth Required', 'Please log in to manage ledger transactions.', 'warning');
            return;
        }
        if (confirm('Delete this ledger record? (Balances will adjust automatically)')) {
            try {
                await apiRequest(`/accounts/${id}`, { method: 'DELETE' });
                showToast('Purged', 'Ledger record purged successfully.');
                const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';
                fetchAccountsTab(searchVal);
                fetchDashboardData();
            } catch (e) { }
        }
    };

    // Financial Modal Handler: Open for Income or Expense
    window.openFinancialModal = function(type = 'INCOME') {
        // Enforce login requirement
        if (!state.token) {
            showToast('Authentication Required', 'Please log in to record Sunday offerings or church expenses.', 'warning');
            return;
        }

        const modal = document.getElementById('recordDonationModal');
        if (!modal) return;

        setFinancialTransactionType(type);

        const dateInput = document.getElementById('donationDate');
        if (dateInput) {
            dateInput.value = new Date().toISOString().split('T')[0];
        }

        modal.style.display = 'flex';
    };

    window.setFinancialTransactionType = function(type) {
        const transTypeInput = document.getElementById('transType');
        const btnIncome = document.getElementById('typeSwitchIncome');
        const btnExpense = document.getElementById('typeSwitchExpense');
        const modalTitle = document.getElementById('transModalTitle');
        const modalSubtitle = document.getElementById('transModalSubtitle');
        const giverLabel = document.getElementById('giverLabel');
        const fundLabel = document.getElementById('fundLabel');
        const memberSelect = document.getElementById('donationMember');
        const customPayee = document.getElementById('customPayeeInput');
        const fundSelect = document.getElementById('donationFund');
        const chipsContainer = document.getElementById('categoryChipsContainer');
        const submitBtn = document.getElementById('btnSubmitTransaction');

        if (!transTypeInput) return;
        transTypeInput.value = type;

        if (type === 'INCOME') {
            if (btnIncome) btnIncome.className = 'type-switch-btn active-income';
            if (btnExpense) btnExpense.className = 'type-switch-btn';
            if (modalTitle) modalTitle.textContent = 'Record Sunday & Special Offering';
            if (modalSubtitle) modalSubtitle.textContent = 'Log Sunday worship collections, tithes, thanksgiving, or mission gifts';
            if (giverLabel) giverLabel.textContent = 'Contributor / Giver Name';
            if (fundLabel) fundLabel.textContent = 'Offering Category / Fund';
            if (memberSelect) memberSelect.style.display = 'block';
            if (customPayee) customPayee.style.display = 'none';
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="ri-check-line"></i> Save Offering';
                submitBtn.style.background = 'linear-gradient(135deg, #0d9488, #14b8a6)';
            }

            if (fundSelect) {
                fundSelect.innerHTML = OFFERING_CATEGORIES.map(c => `<option value="${c}">${c}</option>`).join('');
            }
            if (chipsContainer) {
                chipsContainer.innerHTML = OFFERING_CATEGORIES.map((c, i) =>
                    `<button type="button" class="chip-btn ${i === 0 ? 'active' : ''}" onclick="selectCategoryChip('${c}')">${c}</button>`
                ).join('');
            }
        } else {
            if (btnIncome) btnIncome.className = 'type-switch-btn';
            if (btnExpense) btnExpense.className = 'type-switch-btn active-expense';
            if (modalTitle) modalTitle.textContent = 'Record Church Operating Expense';
            if (modalSubtitle) modalSubtitle.textContent = 'Log utility bills, sanctuary repairs, guest honorariums, or charity aid';
            if (giverLabel) giverLabel.textContent = 'Vendor / Payee / Recipient';
            if (fundLabel) fundLabel.textContent = 'Expense Category';
            if (memberSelect) memberSelect.style.display = 'none';
            if (customPayee) customPayee.style.display = 'block';
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="ri-check-line"></i> Save Expense';
                submitBtn.style.background = 'linear-gradient(135deg, #f43f5e, #ec4899)';
            }

            if (fundSelect) {
                fundSelect.innerHTML = EXPENSE_CATEGORIES.map(c => `<option value="${c}">${c}</option>`).join('');
            }
            if (chipsContainer) {
                chipsContainer.innerHTML = EXPENSE_CATEGORIES.map((c, i) =>
                    `<button type="button" class="chip-btn ${i === 0 ? 'active' : ''}" onclick="selectCategoryChip('${c}')">${c}</button>`
                ).join('');
            }
        }
    };

    window.selectCategoryChip = function(category) {
        const fundSelect = document.getElementById('donationFund');
        if (fundSelect) fundSelect.value = category;

        const chips = document.querySelectorAll('#categoryChipsContainer .chip-btn');
        chips.forEach(chip => {
            if (chip.textContent === category) chip.classList.add('active');
            else chip.classList.remove('active');
        });
    };

    function getLastSundayDate() {
        const d = new Date();
        const day = d.getDay(); // 0 is Sunday
        const diff = day === 0 ? 0 : day;
        d.setDate(d.getDate() - diff);
        return d.toISOString().split('T')[0];
    }

    // Set up ledger filter tab event listeners
    const filterLedgerAll = document.getElementById('filterLedgerAll');
    const filterLedgerIncome = document.getElementById('filterLedgerIncome');
    const filterLedgerExpense = document.getElementById('filterLedgerExpense');

    function setLedgerFilter(filter) {
        currentFinancialFilter = filter;
        [filterLedgerAll, filterLedgerIncome, filterLedgerExpense].forEach(btn => {
            if (btn) btn.classList.remove('active');
        });
        if (filter === 'all' && filterLedgerAll) filterLedgerAll.classList.add('active');
        if (filter === 'INCOME' && filterLedgerIncome) filterLedgerIncome.classList.add('active');
        if (filter === 'EXPENSE' && filterLedgerExpense) filterLedgerExpense.classList.add('active');

        const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';
        fetchAccountsTab(searchVal);
    }

    if (filterLedgerAll) filterLedgerAll.addEventListener('click', () => setLedgerFilter('all'));
    if (filterLedgerIncome) filterLedgerIncome.addEventListener('click', () => setLedgerFilter('INCOME'));
    if (filterLedgerExpense) filterLedgerExpense.addEventListener('click', () => setLedgerFilter('EXPENSE'));

    // Date helper buttons in Transaction Modal
    const btnDateToday = document.getElementById('btnDateToday');
    const btnDateLastSunday = document.getElementById('btnDateLastSunday');
    if (btnDateToday) {
        btnDateToday.addEventListener('click', () => {
            const dateInput = document.getElementById('donationDate');
            if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
        });
    }
    if (btnDateLastSunday) {
        btnDateLastSunday.addEventListener('click', () => {
            const dateInput = document.getElementById('donationDate');
            if (dateInput) dateInput.value = getLastSundayDate();
        });
    }

    // Amount pill quick selector
    document.querySelectorAll('.amount-pill').forEach(pill => {
        pill.addEventListener('click', () => {
            const amt = pill.getAttribute('data-amt');
            const amtInput = document.getElementById('donationAmount');
            if (amtInput && amt) amtInput.value = amt;
        });
    });

    // Transaction type switcher in modal
    const typeSwitchIncome = document.getElementById('typeSwitchIncome');
    const typeSwitchExpense = document.getElementById('typeSwitchExpense');
    if (typeSwitchIncome) typeSwitchIncome.addEventListener('click', () => setFinancialTransactionType('INCOME'));
    if (typeSwitchExpense) typeSwitchExpense.addEventListener('click', () => setFinancialTransactionType('EXPENSE'));


    // ==========================================================================
    // WHATSAPP WISHES & CELEBRATION REMINDERS ENGINE
    // ==========================================================================

    const BIBLE_VERSES_BDAY = [
        '"The LORD bless you and keep you; the LORD make his face shine on you and be gracious to you; the LORD turn his face toward you and give you peace." — Numbers 6:24-26',
        '"For I know the plans I have for you," declares the LORD, "plans to prosper you and not to harm you, plans to give you hope and a future." — Jeremiah 29:11',
        '"May he give you the desire of your heart and make all your plans succeed." — Psalm 20:4',
        '"The LORD your God is with you, the Mighty Warrior who saves. He will take great delight in you; in his love he will rejoice over you with singing." — Zephaniah 3:17'
    ];

    const BIBLE_VERSES_ANNIVERSARY = [
        '"Above all, love each other deeply, because love covers over a multitude of sins." — 1 Peter 4:8',
        '"And over all these virtues put on love, which binds them all together in perfect unity." — Colossians 3:14',
        '"Two are better than one, because they have a good return for their labor... A cord of three strands is not quickly broken." — Ecclesiastes 4:9,12',
        '"Therefore what God has joined together, let no one separate." — Mark 10:9'
    ];

    function cleanPhoneNumber(phone) {
        if (!phone) return '';
        let cleaned = phone.replace(/[^0-9]/g, '');
        if (cleaned.length === 10) {
            cleaned = '91' + cleaned; // Default country code if 10 digits
        }
        return cleaned;
    }

    function calculateAnniversaryYears(marriageDateStr) {
        if (!marriageDateStr) return '';
        try {
            const mDate = new Date(marriageDateStr);
            const today = new Date();
            let years = today.getFullYear() - mDate.getFullYear();
            if (years <= 0) return '1st';
            const suffix = (years % 10 === 1 && years % 100 !== 11) ? 'st' :
                           (years % 10 === 2 && years % 100 !== 12) ? 'nd' :
                           (years % 10 === 3 && years % 100 !== 13) ? 'rd' : 'th';
            return `${years}${suffix}`;
        } catch (e) {
            return '';
        }
    }

    window.openWhatsAppWishModal = function(type, memberId) {
        const member = (state.members || []).find(m => String(m.id) === String(memberId));
        if (!member) {
            showToast('Member Notice', 'Member record not found in cache.', 'warning');
            return;
        }

        const modal = document.getElementById('whatsappWishModal');
        const titleEl = document.getElementById('waModalTitle');
        const subtitleEl = document.getElementById('waModalSubtitle');
        const recipientNameEl = document.getElementById('waRecipientName');
        const recipientPhoneEl = document.getElementById('waRecipientPhone');
        const celebrationTagEl = document.getElementById('waCelebrationTag');
        const messageTextEl = document.getElementById('waMessageText');
        const sendBtn = document.getElementById('waDirectSendBtn');
        const copyBtn = document.getElementById('waCopyBtn');

        const cleanPhone = cleanPhoneNumber(member.mobileNumber);
        recipientNameEl.textContent = `${member.firstName} ${member.lastName}`;
        recipientPhoneEl.textContent = member.mobileNumber ? `📱 Mobile: ${member.mobileNumber}` : '⚠️ No mobile number registered';

        let message = '';
        if (type === 'birthday') {
            titleEl.textContent = 'Send Birthday Blessing via WhatsApp';
            subtitleEl.textContent = `Dispatch personalized scripture birthday wish to ${member.firstName}`;
            celebrationTagEl.textContent = 'Birthday Blessing 🎂';
            celebrationTagEl.style.background = 'rgba(37, 211, 102, 0.12)';
            celebrationTagEl.style.color = '#128C7E';

            const verse = BIBLE_VERSES_BDAY[Math.floor(Math.random() * BIBLE_VERSES_BDAY.length)];
            message = `🎂 Grace & Peace! Dearest ${member.firstName} ${member.lastName},\n\nWishing you a very Blessed and Happy Birthday from your church family at GraceConnect! 🕊️✨\n\n📖 Scripture Blessing for your new year:\n${verse}\n\nMay the LORD surround you with His unfailing love, good health, and joyful favor throughout this coming year!\n\nWith love and prayers,\nGraceConnect Church Family`;
        } else {
            const milestone = calculateAnniversaryYears(member.marriageDate);
            titleEl.textContent = 'Send Anniversary Blessing via WhatsApp';
            subtitleEl.textContent = `Dispatch anniversary greeting and scripture to ${member.firstName} & spouse`;
            celebrationTagEl.textContent = `${milestone || 'Happy'} Anniversary 💍`;
            celebrationTagEl.style.background = 'rgba(244, 63, 94, 0.12)';
            celebrationTagEl.style.color = '#f43f5e';

            const verse = BIBLE_VERSES_ANNIVERSARY[Math.floor(Math.random() * BIBLE_VERSES_ANNIVERSARY.length)];
            message = `💍 Happy ${milestone ? milestone + ' ' : ''}Wedding Anniversary, ${member.firstName} & family! 🥂🕊️\n\nWarmest Christian congratulations on your anniversary from your church family at GraceConnect!\n\n📖 Scripture Blessing for your holy matrimony:\n${verse}\n\nMay God continue to anchor your home in Christ's peace, mutual devotion, and abundant grace for many more blessed years together!\n\nWith love and prayers,\nGraceConnect Church Family`;
        }

        messageTextEl.value = message;

        function updateSendLink() {
            const currentMsg = encodeURIComponent(messageTextEl.value);
            if (cleanPhone) {
                sendBtn.href = `https://wa.me/${cleanPhone}?text=${currentMsg}`;
            } else {
                sendBtn.href = `https://wa.me/?text=${currentMsg}`;
            }
        }
        updateSendLink();
        messageTextEl.oninput = updateSendLink;

        copyBtn.onclick = () => {
            navigator.clipboard.writeText(messageTextEl.value).then(() => {
                showToast('Copied!', 'Blessing message copied to clipboard.');
            }).catch(() => {
                showToast('Copied', 'Ready to paste.');
            });
        };

        if (modal) modal.style.display = 'flex';
    };

    let activeBdayFilter = 'all';
    let activeAnnFilter = 'all';

    // Renders custom lists for birthdays / anniversaries tabs with live WhatsApp triggers
    async function fetchCelebrationRemindersTab(type, search = '') {
        const bdayContainer = document.getElementById('birthdaysListContainer');
        const annContainer = document.getElementById('anniversariesListContainer');

        try {
            const json = await apiRequest('/members?size=300');
            let members = json.data.content;
            state.members = members;

            const today = new Date();
            const currentMonth = today.getMonth() + 1;
            const currentDay = today.getDate();

            if (search.trim() !== '') {
                const term = search.toLowerCase().trim();
                members = members.filter(m => 
                    (m.firstName + ' ' + m.lastName).toLowerCase().includes(term) ||
                    (m.mobileNumber && m.mobileNumber.includes(term))
                );
            }

            if (type === 'birthdays' && bdayContainer) {
                let filtered = members.filter(m => m.dob);

                if (activeBdayFilter === 'today') {
                    filtered = filtered.filter(m => {
                        const parts = m.dob.split('-');
                        return parseInt(parts[1], 10) === currentMonth && parseInt(parts[2], 10) === currentDay;
                    });
                } else if (activeBdayFilter === 'month') {
                    filtered = filtered.filter(m => {
                        const parts = m.dob.split('-');
                        return parseInt(parts[1], 10) === currentMonth;
                    });
                }

                if (filtered.length === 0) {
                    bdayContainer.innerHTML = `
                        <div style="padding: 40px 24px; text-align: center; color: var(--text-light); width: 100%;">
                            <i class="ri-cake-2-line" style="font-size: 38px; color: var(--primary-purple); display: block; margin-bottom: 12px; opacity: 0.7;"></i>
                            <p style="font-size: 14px; font-weight: 500;">No birthdays found matching this filter.</p>
                        </div>
                    `;
                    return;
                }

                bdayContainer.innerHTML = filtered.map(m => {
                    const dobParts = m.dob.split('-');
                    const isToday = parseInt(dobParts[1], 10) === currentMonth && parseInt(dobParts[2], 10) === currentDay;
                    const badgeText = isToday ? '🎉 Birthday Today!' : `DOB: ${m.dob}`;
                    const badgeBg = isToday ? 'rgba(37, 211, 102, 0.15)' : 'rgba(139, 92, 246, 0.08)';
                    const badgeColor = isToday ? '#128C7E' : 'var(--primary-purple)';

                    return `
                        <div class="records-card">
                            <div class="record-avatar">
                                <img src="${m.profilePhoto}" alt="${m.firstName}" onerror="this.src='https://placehold.co/100/8b5cf6/ffffff?text=${m.firstName[0]}'">
                            </div>
                            <div class="record-info" style="flex:1;">
                                <div style="display:flex; justify-content:space-between; align-items:center;">
                                    <span class="record-title">${m.firstName} ${m.lastName}</span>
                                    <span class="record-tag" style="background: ${badgeBg}; color: ${badgeColor}; font-weight:700;">
                                        ${badgeText}
                                    </span>
                                </div>
                                <span class="record-subtitle"><i class="ri-phone-line"></i> ${m.mobileNumber || 'No phone'}</span>
                                <div style="margin-top:12px; display:flex; gap:8px; align-items:center;">
                                    <button class="btn-whatsapp btn-whatsapp-sm" onclick="openWhatsAppWishModal('birthday', '${m.id}')">
                                        <i class="ri-whatsapp-line"></i> WhatsApp Wish
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('');
            }

            if (type === 'anniversaries' && annContainer) {
                let married = members.filter(m => m.marriageStatus === 'Married' && m.marriageDate);

                if (activeAnnFilter === 'today') {
                    married = married.filter(m => {
                        const parts = m.marriageDate.split('-');
                        return parseInt(parts[1], 10) === currentMonth && parseInt(parts[2], 10) === currentDay;
                    });
                } else if (activeAnnFilter === 'month') {
                    married = married.filter(m => {
                        const parts = m.marriageDate.split('-');
                        return parseInt(parts[1], 10) === currentMonth;
                    });
                }

                if (married.length === 0) {
                    annContainer.innerHTML = `
                        <div style="padding: 40px 24px; text-align: center; color: var(--text-light); width: 100%;">
                            <i class="ri-hearts-line" style="font-size: 38px; color: var(--primary-rose); display: block; margin-bottom: 12px; opacity: 0.7;"></i>
                            <p style="font-size: 14px; font-weight: 500;">No marriage anniversaries found matching this filter.</p>
                        </div>
                    `;
                    return;
                }

                annContainer.innerHTML = married.map(m => {
                    const mParts = m.marriageDate.split('-');
                    const isToday = parseInt(mParts[1], 10) === currentMonth && parseInt(mParts[2], 10) === currentDay;
                    const milestone = calculateAnniversaryYears(m.marriageDate);
                    const badgeText = isToday ? `💍 Anniversary Today! (${milestone})` : `${milestone || ''} Anniversary • ${m.marriageDate}`;

                    return `
                        <div class="records-card">
                            <div class="record-avatar" style="background: rgba(244, 63, 94, 0.08); display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--primary-rose)">
                                <i class="ri-hearts-line"></i>
                            </div>
                            <div class="record-info" style="flex:1;">
                                <div style="display:flex; justify-content:space-between; align-items:center;">
                                    <span class="record-title">${m.firstName} ${m.lastName} & Family</span>
                                    <span class="record-tag" style="background: rgba(244, 63, 94, 0.1); color: var(--primary-rose); font-weight:700;">
                                        ${badgeText}
                                    </span>
                                </div>
                                <span class="record-subtitle"><i class="ri-calendar-line"></i> Married on: ${m.marriageDate} • 📱 ${m.mobileNumber || 'No phone'}</span>
                                <div style="margin-top:12px; display:flex; gap:8px;">
                                    <button class="btn-whatsapp btn-whatsapp-sm" onclick="openWhatsAppWishModal('anniversary', '${m.id}')">
                                        <i class="ri-whatsapp-line"></i> WhatsApp Wish
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        } catch (e) { }
    }

    // Birthday and Anniversary filter button triggers
    document.querySelectorAll('#bdayFilterGroup [data-bday-filter]').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#bdayFilterGroup [data-bday-filter]').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeBdayFilter = btn.getAttribute('data-bday-filter');
            fetchCelebrationRemindersTab('birthdays');
        });
    });

    document.querySelectorAll('#annFilterGroup [data-ann-filter]').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#annFilterGroup [data-ann-filter]').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeAnnFilter = btn.getAttribute('data-ann-filter');
            fetchCelebrationRemindersTab('anniversaries');
        });
    });

    // Celebrations Sidebar Widget
    const celebrationTabBirthdays = document.getElementById('celebrationTabBirthdays');
    const celebrationTabAnniversaries = document.getElementById('celebrationTabAnniversaries');
    const celebrationList = document.getElementById('celebrationList');

    async function populateCelebrationWidget(mode) {
        if (!celebrationList) return;
        celebrationList.innerHTML = '';

        try {
            const json = await apiRequest('/members?size=200');
            const members = json.data.content;
            state.members = members;

            const today = new Date();
            const curMonth = today.getMonth() + 1;
            const curDay = today.getDate();

            if (mode === 'birthdays') {
                if (celebrationTabBirthdays) celebrationTabBirthdays.classList.add('active');
                if (celebrationTabAnniversaries) celebrationTabAnniversaries.classList.remove('active');

                // Sort: birthdays today first, then this month, then others
                const bdays = members.filter(m => m.dob).sort((a, b) => {
                    const aP = a.dob.split('-');
                    const bP = b.dob.split('-');
                    const aIsToday = parseInt(aP[1], 10) === curMonth && parseInt(aP[2], 10) === curDay ? 0 : 1;
                    const bIsToday = parseInt(bP[1], 10) === curMonth && parseInt(bP[2], 10) === curDay ? 0 : 1;
                    return aIsToday - bIsToday;
                });

                celebrationList.innerHTML = bdays.slice(0, 5).map(m => {
                    const dobParts = m.dob.split('-');
                    const isToday = parseInt(dobParts[1], 10) === curMonth && parseInt(dobParts[2], 10) === curDay;

                    return `
                        <div class="reminder-item">
                            <div class="reminder-avatar">
                                <img src="${m.profilePhoto}" alt="${m.firstName}" onerror="this.src='https://placehold.co/100/8b5cf6/ffffff?text=${m.firstName[0]}'">
                            </div>
                            <div class="reminder-details" style="flex:1;">
                                <span class="reminder-name">${m.firstName} ${m.lastName}</span>
                                <span class="reminder-date">
                                    <i class="ri-cake-2-line"></i> ${isToday ? '<strong style="color:#128C7E;">🎉 Today!</strong>' : m.dob}
                                </span>
                            </div>
                            <button class="reminder-action-btn" title="Send WhatsApp Blessing" style="background:#25D366; color:white; border-color:#25D366;" onclick="openWhatsAppWishModal('birthday', '${m.id}')">
                                <i class="ri-whatsapp-line"></i>
                            </button>
                        </div>
                    `;
                }).join('');
            } else {
                if (celebrationTabBirthdays) celebrationTabBirthdays.classList.remove('active');
                if (celebrationTabAnniversaries) celebrationTabAnniversaries.classList.add('active');

                const married = members.filter(m => m.marriageStatus === 'Married' && m.marriageDate);
                celebrationList.innerHTML = married.slice(0, 5).map(m => {
                    const mParts = m.marriageDate.split('-');
                    const isToday = parseInt(mParts[1], 10) === curMonth && parseInt(mParts[2], 10) === curDay;
                    const milestone = calculateAnniversaryYears(m.marriageDate);

                    return `
                        <div class="reminder-item">
                            <div class="reminder-avatar" style="background: rgba(244, 63, 94, 0.08); display: flex; align-items: center; justify-content: center; font-size: 16px; color: var(--primary-rose)">
                                <i class="ri-hearts-line"></i>
                            </div>
                            <div class="reminder-details" style="flex:1;">
                                <span class="reminder-name">${m.firstName} & Spouse</span>
                                <span class="reminder-date">
                                    <i class="ri-calendar-line"></i> ${isToday ? '<strong style="color:var(--primary-rose);">💍 Today!</strong>' : (milestone ? milestone + ' Anniv.' : m.marriageDate)}
                                </span>
                            </div>
                            <button class="reminder-action-btn" title="Send Anniversary WhatsApp Wish" style="background:#25D366; color:white; border-color:#25D366;" onclick="openWhatsAppWishModal('anniversary', '${m.id}')">
                                <i class="ri-whatsapp-line"></i>
                            </button>
                        </div>
                    `;
                }).join('');
            }
        } catch (e) { }
    }

    if (celebrationTabBirthdays && celebrationTabAnniversaries) {
        celebrationTabBirthdays.addEventListener('click', () => populateCelebrationWidget('birthdays'));
        celebrationTabAnniversaries.addEventListener('click', () => populateCelebrationWidget('anniversaries'));
    }

    populateCelebrationWidget('birthdays');

    // Static page renders for mocks
    function renderRemindersTab() {
        const container = document.getElementById('remindersListContainer');
        if (!container) return;
        container.innerHTML = `
            <div class="records-card">
                <div class="record-avatar" style="background: rgba(245, 158, 11, 0.08); display: flex; align-items: center; justify-content: center; font-size: 22px; color: #d97706">
                    <i class="ri-notification-3-line"></i>
                </div>
                <div class="record-info">
                    <span class="record-title">Pentecost Sunday Music Prep</span>
                    <span class="record-subtitle">Coordinate with the deacon council and choir coordinators.</span>
                    <span class="record-tag" style="background: rgba(245, 158, 11, 0.08); color: #d97706">High Priority</span>
                </div>
            </div>
            <div class="records-card">
                <div class="record-avatar" style="background: rgba(59, 130, 246, 0.08); display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--primary-blue)">
                    <i class="ri-mail-send-line"></i>
                </div>
                <div class="record-info">
                    <span class="record-title">Send welcome booklet to new families</span>
                    <span class="record-subtitle">Prepare community introduction envelopes and schedules.</span>
                    <span class="record-tag" style="background: rgba(59, 130, 246, 0.08); color: var(--primary-blue)">Pending Action</span>
                </div>
            </div>
        `;
    }

    function renderReportsTab() {
        const container = document.getElementById('reportsListContainer');
        if (!container) return;
        container.innerHTML = `
            <div class="records-card">
                <div class="record-info">
                    <span class="record-title">Quarterly Congregation Attendance Roll</span>
                    <span class="record-subtitle">Detailed analyses of weekly worship assemblies.</span>
                    <button class="subpage-btn" style="margin-top: 10px;" onclick="alert('Exporting PDF...')"><i class="ri-file-download-line"></i> Export PDF</button>
                </div>
            </div>
            <div class="records-card">
                <div class="record-info">
                    <span class="record-title">Annual Charitable Contribution Ledger</span>
                    <span class="record-subtitle">Audit sheet of financial collections, tithes, and missionary outreach accounts.</span>
                    <button class="subpage-btn" style="margin-top: 10px;" onclick="alert('Exporting CSV...')"><i class="ri-file-download-line"></i> Export Excel CSV</button>
                </div>
            </div>
        `;
    }

    function renderSettingsTab() {
        const container = document.getElementById('settingsListContainer');
        if (!container) return;
        container.innerHTML = `
            <div style="grid-column: span 2; background: white; padding: 24px; border-radius: var(--radius-lg); border: 1px solid var(--border-glass)">
                <h3 style="font-family:'Outfit'; margin-bottom: 16px;">General Configuration</h3>
                <div class="form-group">
                    <label>Church Name</label>
                    <input type="text" class="form-control" value="GraceConnect Family Church" readonly>
                </div>
                <div class="form-row" style="margin-top: 12px;">
                    <div class="form-group">
                        <label>Currency Symbol</label>
                        <input type="text" class="form-control" value="INR (₹)" readonly>
                    </div>
                    <div class="form-group">
                        <label>Active Database Type</label>
                        <input type="text" class="form-control" value="MySQL Database Server" readonly>
                    </div>
                </div>
                <button class="subpage-btn primary" style="margin-top: 20px;" onclick="alert('Settings configured!')">Save Configurations</button>
            </div>
        `;
    }

    // ==========================================================================
    // 6. HIGH-DPI BEZIER CANVAS CHART DRAWING
    // ==========================================================================

    function renderDashboardChart(months, incomeData, expenseData) {
        const canvas = document.getElementById('chartCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        // Support High-DPI screens
        const dpr = window.devicePixelRatio || 1;
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * dpr;
        canvas.height = 200 * dpr;
        ctx.scale(dpr, dpr);

        const width = rect.width;
        const height = 200;

        const padding = { left: 40, right: 20, top: 20, bottom: 30 };
        const graphWidth = width - padding.left - padding.right;
        const graphHeight = height - padding.top - padding.bottom;

        ctx.clearRect(0, 0, width, height);

        // Standard max boundaries
        const maxValue = 22000;

        // 1. Draw dashed grid lines and Y-axis labels
        ctx.strokeStyle = 'rgba(226, 232, 240, 0.8)';
        ctx.setLineDash([4, 4]);
        ctx.lineWidth = 1;
        ctx.font = '10px Inter';
        ctx.fillStyle = '#64748b';

        const yTicks = 4;
        for (let i = 0; i <= yTicks; i++) {
            const yVal = (maxValue / yTicks) * i;
            const y = padding.top + graphHeight - (yVal / maxValue) * graphHeight;

            ctx.beginPath();
            ctx.moveTo(padding.left, y);
            ctx.lineTo(width - padding.right, y);
            ctx.stroke();

            ctx.setLineDash([]);
            ctx.fillText(`$${(yVal / 1000).toFixed(0)}k`, padding.left - 34, y + 3);
            ctx.setLineDash([4, 4]);
        }
        ctx.setLineDash([]);

        function getCoords(index, val) {
            const x = padding.left + (index / (months.length - 1)) * graphWidth;
            const y = padding.top + graphHeight - (val / maxValue) * graphHeight;
            return { x, y };
        }

        function drawBezierCurve(dataPoints, strokeColor, fillColor) {
            ctx.beginPath();
            const firstPt = getCoords(0, dataPoints[0]);
            ctx.moveTo(firstPt.x, firstPt.y);

            for (let i = 0; i < dataPoints.length - 1; i++) {
                const current = getCoords(i, dataPoints[i]);
                const next = getCoords(i + 1, dataPoints[i + 1]);

                const controlX1 = current.x + (next.x - current.x) / 2;
                const controlY1 = current.y;
                const controlX2 = current.x + (next.x - current.x) / 2;
                const controlY2 = next.y;

                ctx.bezierCurveTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y);
            }

            ctx.strokeStyle = strokeColor;
            ctx.lineWidth = 3;
            ctx.stroke();

            ctx.lineTo(width - padding.right, padding.top + graphHeight);
            ctx.lineTo(padding.left, padding.top + graphHeight);
            ctx.closePath();

            const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + graphHeight);
            gradient.addColorStop(0, fillColor);
            gradient.addColorStop(1, 'rgba(255, 255, 255, 0.0)');
            ctx.fillStyle = gradient;
            ctx.fill();
        }

        drawBezierCurve(incomeData, '#8b5cf6', 'rgba(139, 92, 246, 0.15)');
        drawBezierCurve(expenseData, '#38bdf8', 'rgba(56, 189, 248, 0.15)');

        months.forEach((month, index) => {
            const incPt = getCoords(index, incomeData[index]);
            const expPt = getCoords(index, expenseData[index]);

            ctx.font = '500 10px Inter';
            ctx.fillStyle = '#64748b';
            ctx.fillText(month, incPt.x - 10, padding.top + graphHeight + 18);

            // Purple Dot
            ctx.beginPath();
            ctx.arc(incPt.x, incPt.y, 4, 0, 2 * Math.PI);
            ctx.fillStyle = '#8b5cf6';
            ctx.fill();
            ctx.lineWidth = 2;
            ctx.strokeStyle = 'white';
            ctx.stroke();

            // Blue Dot
            ctx.beginPath();
            ctx.arc(expPt.x, expPt.y, 4, 0, 2 * Math.PI);
            ctx.fillStyle = '#38bdf8';
            ctx.fill();
            ctx.lineWidth = 2;
            ctx.strokeStyle = 'white';
            ctx.stroke();
        });
    }

    // ==========================================================================
    // 7. DYNAMIC CALENDAR WIDGET
    // ==========================================================================

    const calendarDatesGrid = document.getElementById('calendarDatesGrid');
    const calendarMonthYear = document.getElementById('calendarMonthYear');
    const prevMonthBtn = document.getElementById('prevMonthBtn');
    const nextMonthBtn = document.getElementById('nextMonthBtn');
    let calendarDate = new Date(2026, 4, 25); // Set to May 2026 baseline

    async function renderCalendar() {
        if (!calendarDatesGrid || !calendarMonthYear) return;

        calendarDatesGrid.innerHTML = '';

        const year = calendarDate.getFullYear();
        const month = calendarDate.getMonth();
        const monthsList = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];

        calendarMonthYear.textContent = `${monthsList[month]} ${year}`;

        const firstDayIndex = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const daysInPrevMonth = new Date(year, month, 0).getDate();

        // 1. Fetch calendar events for this month from database
        let activeDates = [];
        try {
            const startStr = `${year}-${String(month + 1).padStart(2, '0')}-01`;
            const endStr = `${year}-${String(month + 1).padStart(2, '0')}-${daysInMonth}`;
            const json = await apiRequest(`/events/calendar?start=${startStr}&end=${endStr}`);
            if (json.data) {
                activeDates = json.data.map(ev => ev.date);
            }
        } catch (e) { }

        // Populate days of previous month leading to first day
        for (let x = firstDayIndex; x > 0; x--) {
            const cell = document.createElement('div');
            cell.classList.add('calendar-date-cell', 'other-month');
            cell.textContent = daysInPrevMonth - x + 1;
            calendarDatesGrid.appendChild(cell);
        }

        // Populate current month's days
        for (let i = 1; i <= daysInMonth; i++) {
            const cell = document.createElement('div');
            cell.classList.add('calendar-date-cell');
            cell.textContent = i;

            // Highlight Today (May 25, 2026)
            if (i === 25 && month === 4 && year === 2026) {
                cell.classList.add('today');
            }

            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
            if (activeDates.includes(dateStr)) {
                cell.classList.add('active-event');
            }

            cell.addEventListener('click', () => {
                document.querySelectorAll('.calendar-date-cell').forEach(c => c.classList.remove('active'));
                cell.classList.add('active');
                showToast('Date Selected', `Viewing schedules for ${monthsList[month]} ${i}, ${year}.`, 'info');
            });

            calendarDatesGrid.appendChild(cell);
        }

        // Fill standard 6-row calendar cells
        const totalCells = calendarDatesGrid.children.length;
        const fillCellsNeeded = 42 - totalCells;
        for (let z = 1; z <= fillCellsNeeded; z++) {
            const cell = document.createElement('div');
            cell.classList.add('calendar-date-cell', 'other-month');
            cell.textContent = z;
            calendarDatesGrid.appendChild(cell);
        }
    }

    if (prevMonthBtn && nextMonthBtn) {
        prevMonthBtn.addEventListener('click', () => {
            calendarDate.setMonth(calendarDate.getMonth() - 1);
            renderCalendar();
        });
        nextMonthBtn.addEventListener('click', () => {
            calendarDate.setMonth(calendarDate.getMonth() + 1);
            renderCalendar();
        });
    }

    // ==========================================================================
    // 8. TOAST NOTIFICATION ENGINE
    // ==========================================================================

    function showToast(title, desc, type = 'success') {
        const container = document.getElementById('toastContainer');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        let icon = 'ri-checkbox-circle-line';
        if (type === 'info') icon = 'ri-information-line';
        if (type === 'warning') icon = 'ri-error-warning-line';

        toast.innerHTML = `
            <i class="toast-icon ${icon}"></i>
            <div class="toast-body">
                <span class="toast-title">${title}</span>
                <p class="toast-desc">${desc}</p>
            </div>
        `;

        container.appendChild(toast);

        // Slide away and delete
        setTimeout(() => {
            toast.style.animation = 'slideInLeft var(--transition-normal) reverse forwards';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // ==========================================================================
    // 9. MODALS MANAGER & DYNAMIC REST FORM SUBMISSIONS
    // ==========================================================================

    const modals = {
        addFamily: document.getElementById('addFamilyModal'),
        addMember: document.getElementById('addMemberModal'),
        recordDonation: document.getElementById('recordDonationModal'),
        addEvent: document.getElementById('addEventModal'),
        whatsappWish: document.getElementById('whatsappWishModal')
    };

    function openModal(modalId) {
        const modal = modals[modalId];
        if (modal) {
            modal.style.display = 'flex';
            refreshDropdowns();
        }
    }

    function closeModal(modalId) {
        const modal = modals[modalId];
        if (modal) {
            modal.style.display = 'none';
        }
    }

    // Modal click triggers
    const modalTriggers = [
        { btn: 'quickActionAddFamily', modal: 'addFamily' },
        { btn: 'quickActionScheduleEvent', modal: 'addEvent' },
        { btn: 'addFamilyNavbarBtn', modal: 'addFamily' },
        { btn: 'scheduleEventBtn', modal: 'addEvent' }
    ];

    modalTriggers.forEach(trigger => {
        const btn = document.getElementById(trigger.btn);
        if (btn) {
            btn.addEventListener('click', () => openModal(trigger.modal));
        }
    });

    // Dedicated Financial Ledger Triggers (Offerings vs Church Expenses)
    const btnQuickOffering = document.getElementById('quickActionSundayOffering');
    const btnQuickExpense = document.getElementById('quickActionChurchExpense');
    const btnNavOffering = document.getElementById('recordOfferingNavbarBtn');
    const btnNavExpense = document.getElementById('recordExpenseNavbarBtn');

    if (btnQuickOffering) btnQuickOffering.addEventListener('click', () => openFinancialModal('INCOME'));
    if (btnQuickExpense) btnQuickExpense.addEventListener('click', () => openFinancialModal('EXPENSE'));
    if (btnNavOffering) btnNavOffering.addEventListener('click', () => openFinancialModal('INCOME'));
    if (btnNavExpense) btnNavExpense.addEventListener('click', () => openFinancialModal('EXPENSE'));

    document.querySelectorAll('[data-close]').forEach(btn => {
        btn.addEventListener('click', () => {
            const modalId = btn.getAttribute('data-close');
            closeModal(modalId.replace('Modal', ''));
        });
    });

    window.addEventListener('click', (e) => {
        Object.keys(modals).forEach(key => {
            const modal = modals[key];
            if (e.target === modal) closeModal(key);
        });
    });

    // FORM 1: Create Family Submit
    const addFamilyForm = document.getElementById('addFamilyForm');
    const toggleAddMembers = document.getElementById('toggleAddMembers');
    const addMembersSection = document.getElementById('addMembersSection');

    if (toggleAddMembers && addMembersSection) {
        toggleAddMembers.addEventListener('change', () => {
            const isChecked = toggleAddMembers.checked;
            addMembersSection.style.display = isChecked ? 'block' : 'none';
            
            const fields = ['famMemberFirstName', 'famMemberLastName', 'famMemberDob', 'famMemberGender'];
            fields.forEach(id => {
                const el = document.getElementById(id);
                if (el) el.required = isChecked;
            });
        });
    }

    if (addFamilyForm) {
        addFamilyForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('familyName').value;
            const address = document.getElementById('familyAddress').value;
            const email = document.getElementById('familyEmail').value;
            const phone = document.getElementById('familyPhone').value;

            try {
                const familyResponse = await apiRequest('/families', {
                    method: 'POST',
                    body: JSON.stringify({ name: `${name} Household`, address, email, phone })
                });

                const createdFamily = familyResponse.data;
                let memberCreatedMsg = "";

                if (toggleAddMembers && toggleAddMembers.checked) {
                    const famMemberFirstName = document.getElementById('famMemberFirstName').value;
                    const famMemberLastName = document.getElementById('famMemberLastName').value;
                    const famMemberDob = document.getElementById('famMemberDob').value;
                    const famMemberGender = document.getElementById('famMemberGender').value;
                    const famMemberMarriageStatus = document.getElementById('famMemberMarriageStatus').value;
                    const famMemberBaptismStatus = document.getElementById('famMemberBaptismStatus').value;
                    
                    const profilePhoto = `https://placehold.co/150/8b5cf6/ffffff?text=${famMemberFirstName[0]}`;

                    await apiRequest('/members', {
                        method: 'POST',
                        body: JSON.stringify({
                            firstName: famMemberFirstName,
                            lastName: famMemberLastName,
                            familyId: createdFamily.id,
                            dob: famMemberDob,
                            gender: famMemberGender,
                            marriageStatus: famMemberMarriageStatus,
                            baptismStatus: famMemberBaptismStatus,
                            profilePhoto
                        })
                    });
                    memberCreatedMsg = ` and added first member ${famMemberFirstName} ${famMemberLastName}`;
                }

                closeModal('addFamily');
                addFamilyForm.reset();
                if (toggleAddMembers) {
                    toggleAddMembers.checked = false;
                    addMembersSection.style.display = 'none';
                }
                
                showToast('Household Registered', `Created entry for "${name} Household"${memberCreatedMsg} in directories.`);

                // Refresh active directory
                if (currentActivePage === 'families') fetchFamiliesTab();
                else if (currentActivePage === 'dashboard') fetchDashboardData();

            } catch (e) { }
        });
    }

    // FORM 2: Create Member Submit
    const addMemberForm = document.getElementById('addMemberForm');
    if (addMemberForm) {
        addMemberForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const firstName = document.getElementById('memberFirstName').value;
            const lastName = document.getElementById('memberLastName').value;
            const familyId = document.getElementById('memberFamilyRef').value;
            const dob = document.getElementById('memberDob').value;
            const gender = document.getElementById('memberGender').value;

            // Generate a modern unsplash mock profile picture
            const profilePhoto = `https://placehold.co/150/8b5cf6/ffffff?text=${firstName[0]}`;

            try {
                await apiRequest('/members', {
                    method: 'POST',
                    body: JSON.stringify({
                        firstName,
                        lastName,
                        familyId,
                        dob,
                        gender,
                        profilePhoto,
                        occupation: 'Congregant',
                        marriageStatus: 'Single',
                        baptismStatus: 'Baptized'
                    })
                });

                closeModal('addMember');
                addMemberForm.reset();
                showToast('Member Added', `Successfully added ${firstName} ${lastName} to roster.`);

                // Refresh active directory
                const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '';
                fetchFamiliesTab(searchVal);

            } catch (e) { }
        });
    }

    // FORM 3: Record Offering or Church Expense Submit
    const recordDonationForm = document.getElementById('recordDonationForm');
    if (recordDonationForm) {
        recordDonationForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            // Strict Authentication Guard
            if (!state.token) {
                showToast('Authentication Required', 'Please log in to record church offerings and expenses.', 'warning');
                return;
            }

            const transType = (document.getElementById('transType').value || 'INCOME').toUpperCase();
            let memberName = '';
            if (transType === 'INCOME') {
                const sel = document.getElementById('donationMember');
                memberName = sel ? sel.value : 'General Sunday Congregation';
            } else {
                const payeeInput = document.getElementById('customPayeeInput');
                memberName = payeeInput && payeeInput.value.trim() !== '' ? payeeInput.value.trim() : 'Church Operations Vendor';
            }

            const amount = parseFloat(document.getElementById('donationAmount').value);
            if (isNaN(amount) || amount <= 0) {
                showToast('Invalid Amount', 'Please specify a valid financial amount greater than 0.', 'warning');
                return;
            }

            const fund = document.getElementById('donationFund').value;
            const dateInput = document.getElementById('donationDate');
            const date = (dateInput && dateInput.value) ? dateInput.value : new Date().toISOString().split('T')[0];
            const notes = document.getElementById('donationNotes').value || (transType === 'INCOME' ? 'Sunday Offering / Tithe' : 'Church Operational Expense');

            try {
                await apiRequest('/accounts', {
                    method: 'POST',
                    body: JSON.stringify({
                        type: transType,
                        memberName,
                        amount,
                        fund,
                        notes,
                        date
                    })
                });

                closeModal('recordDonation');
                recordDonationForm.reset();
                showToast(
                    transType === 'INCOME' ? 'Offering Logged' : 'Expense Recorded',
                    `Successfully recorded ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })} for ${fund}.`
                );

                if (currentActivePage === 'accounts') fetchAccountsTab();
                else if (currentActivePage === 'dashboard') fetchDashboardData();

            } catch (e) { }
        });
    }

    // FORM 4: Create Event Submit
    const addEventForm = document.getElementById('addEventForm');
    if (addEventForm) {
        addEventForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const title = document.getElementById('eventTitle').value;
            const date = document.getElementById('eventDate').value;
            const category = document.getElementById('eventCategory').value;
            const time = document.getElementById('eventTime').value;
            const location = document.getElementById('eventLocation').value;

            try {
                await apiRequest('/events', {
                    method: 'POST',
                    body: JSON.stringify({ title, date, category, time, location })
                });

                closeModal('addEvent');
                addEventForm.reset();
                showToast('Event Scheduled', `Successfully locked community calendar for "${title}".`);

                renderCalendar(); // redraw dots
                if (currentActivePage === 'events') fetchEventsTab();
                else if (currentActivePage === 'dashboard') fetchDashboardData();

            } catch (e) { }
        });
    }

    // Notification Bar click mock
    const notificationBtn = document.getElementById('notificationBtn');
    if (notificationBtn) {
        notificationBtn.addEventListener('click', () => {
            const badge = notificationBtn.querySelector('.notification-badge');
            if (badge) badge.remove();
            showToast('Alert Inbox Checked', 'No unread administrative notifications.', 'info');
        });
    }

    const refreshActivityBtn = document.getElementById('refreshActivityBtn');
    if (refreshActivityBtn) {
        refreshActivityBtn.addEventListener('click', () => {
            if (currentActivePage === 'dashboard') {
                fetchDashboardData();
                showToast('Timeline Refreshed', 'Retrieved recent administrative logs.', 'info');
            }
        });
    }

    // ==========================================================================
    // 10. SYSTEM LAUNCH & INITIALIZATION
    // ==========================================================================
    checkAuth();
    renderCalendar();
    window.addEventListener('resize', () => {
        if (currentActivePage === 'dashboard' && state.dashboardData) {
            renderDashboardChart(
                state.dashboardData.chartMonths,
                state.dashboardData.incomeChartData,
                state.dashboardData.expenseChartData
            );
        }
    });
});
