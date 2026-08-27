const MOBILE_REPO = 'qvietdpzai/Btn-Launcher';
const PC_REPO = 'qvietdpzai/Btnlaucher2-pc';

// Map file patterns to platforms
const PC_PATTERNS = {
    windows: [/win32-x64\.zip$/, /win32-ia32\.zip$/, /\.appx$/],
    macos:   [/x64\.dmg$/, /arm64\.dmg$/, /mac.*\.asar\.gz$/],
    linux:   [/\.deb$/, /\.rpm$/, /\.AppImage$/, /\.pacman$/, /\.tar\.xz$/]
};

const PC_LABELS = {
    windows: 'Windows',
    macos: 'macOS',
    linux: 'Linux'
};

const PC_NOTES = {
    windows: 'Có thể cần bật "Unknown sources" trong Windows Defender',
    macos: 'Có thể cần: chuột phải → Open để bỏ qua Gatekeeper',
    linux: 'Debian/Ubuntu: .deb · Fedora: .rpm · Arch: .pacman · Portable: AppImage'
};

function detectOS() {
    const p = (navigator.userAgent || '').toLowerCase();
    if (p.includes('win')) return 'windows';
    if (p.includes('mac')) return 'macos';
    if (p.includes('android')) return 'windows'; // Android UA contains "Linux"
    if (p.includes('linux')) return 'linux';
    return 'windows'; // default
}

function pickAsset(assets, os) {
    const patterns = PC_PATTERNS[os] || PC_PATTERNS.windows;
    // Prefer installer formats over portable
    const preferred = {
        windows: [/\.appx$/, /win32-x64\.zip$/],
        macos: [/x64\.dmg$/, /arm64\.dmg$/],
        linux: [/\.deb$/, /\.AppImage$/, /\.rpm$/, /\.pacman$/]
    }[os] || [];
    for (const re of preferred) {
        const found = assets.find(a => re.test(a.name));
        if (found) return found;
    }
    for (const re of patterns) {
        const found = assets.find(a => re.test(a.name));
        if (found) return found;
    }
    return assets[0];
}

async function fetchRelease(repo) {
    const res = await fetch(`https://api.github.com/repos/${repo}/releases?per_page=5`);
    if (!res.ok) throw new Error('GitHub API error');
    const releases = await res.json();
    return releases.find(r => !r.prerelease && !r.draft) || releases[0];
}

function formatCount(n) {
    if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M';
    if (n >= 1e3) return (n / 1e3).toFixed(1) + 'K';
    return n;
}

async function init() {
    let os = detectOS();
    setActiveTab(os);

    try {
        // Mobile
        const mRel = await fetchRelease(MOBILE_REPO);
        const mAsset = mRel.assets.find(a => /\.apk$/.test(a.name));
        if (mAsset) {
            document.getElementById('mobile-download').href = mAsset.browser_download_url;
            document.getElementById('mobile-version').textContent = `Phiên bản ${mRel.tag_name} · ${(mAsset.size/1048576).toFixed(0)} MB`;
        }

        // PC
        const pRel = await fetchRelease(PC_REPO);
        const updatePc = () => {
            const asset = pickAsset(pRel.assets, os);
            if (asset) {
                document.getElementById('pc-download').href = asset.browser_download_url;
                document.getElementById('pc-os-label').textContent = PC_LABELS[os];
                document.getElementById('pc-platform-note').textContent = PC_NOTES[os];
                document.getElementById('pc-version').textContent = `Phiên bản ${pRel.tag_name} · ${(asset.size/1048576).toFixed(0)} MB`;
            }
        };
        updatePc();
        document.querySelectorAll('.os-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                setActiveTab(tab.dataset.os);
                os = tab.dataset.os;
                updatePc();
            });
        });

        // Total downloads
        const total = (mRel.assets || []).reduce((s,a)=>s+(a.download_count||0),0)
                    + (pRel.assets || []).reduce((s,a)=>s+(a.download_count||0),0);
        document.getElementById('total-downloads').textContent = formatCount(total);

    } catch (e) {
        document.getElementById('mobile-version').textContent = 'Lỗi tải thông tin';
        document.getElementById('pc-version').textContent = 'Lỗi tải thông tin';
        console.error(e);
    }
}

function setActiveTab(os) {
    document.querySelectorAll('.os-tab').forEach(t => {
        t.classList.toggle('active', t.dataset.os === os);
    });
}

init();
