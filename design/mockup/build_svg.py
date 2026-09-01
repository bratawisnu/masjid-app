"""Generates the four design mockups as self-contained 1920x1080 SVGs.

Written as a generator rather than by hand because all four share one grid,
one type scale, and one set of prayer data — hand-editing four copies is how
mockups drift out of agreement with each other and with the app.

Every dimension below is expressed the way the app expresses it: as a
fraction of the 1080px panel height, so what is drawn here is what Scale.kt
would produce on a real panel.
"""
import os

W, H = 1920, 1080
OUT = os.path.dirname(os.path.abspath(__file__))

SANS = "Avenir Next Condensed, Avenir Next, Helvetica Neue, sans-serif"
ARAB = "Baghdad, Geeza Pro, serif"
MONO = "Menlo, monospace"


def esc(t):
    return (t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def txt(x, y, s, size, fill, *, font=SANS, weight=400, anchor="start",
        spacing=0, opacity=1):
    ls = f' letter-spacing="{spacing:.2f}"' if spacing else ""
    op = f' opacity="{opacity}"' if opacity != 1 else ""
    return (f'<text x="{x}" y="{y}" font-family="{font}" font-size="{size}" '
            f'font-weight="{weight}" fill="{fill}" text-anchor="{anchor}"'
            f'{ls}{op}>{esc(s)}</text>')


def rect(x, y, w, h, fill, *, rx=0, opacity=1):
    op = f' opacity="{opacity}"' if opacity != 1 else ""
    r = f' rx="{rx}"' if rx else ""
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}"{r}{op}/>'


def arch(x, y, w, h, fill, *, opacity=1, spring=0.42):
    """A mihrab bay: vertical jambs rising into a pointed-round head.

    `spring` is where the arch springs from, as a fraction of the bay height.
    The shape is the app's signature element, so it is drawn from real
    geometry rather than approximated with a border-radius.
    """
    sy = y + h * spring
    op = f' opacity="{opacity}"' if opacity != 1 else ""
    d = (f"M{x},{y+h} L{x},{sy} "
         f"C{x},{y + h*spring*0.30} {x + w*0.22},{y} {x + w/2},{y} "
         f"C{x + w*0.78},{y} {x+w},{y + h*spring*0.30} {x+w},{sy} "
         f"L{x+w},{y+h} Z")
    return f'<path d="{d}" fill="{fill}"{op}/>'


PRAYERS = [
    ("IMSAK", "إمساك", "04:28"),
    ("SUBUH", "الفجر", "04:38"),
    ("TERBIT", "الشروق", "05:53"),
    ("DZUHUR", "الظهر", "11:56"),
    ("ASHAR", "العصر", "15:14"),
    ("MAGHRIB", "المغرب", "17:52"),
    ("ISYA", "العشاء", "19:02"),
]

TICKER = ("Infak pembangunan menara telah mencapai Rp 412.750.000 — terima kasih "
          "atas dukungan jamaah   ·   Kerja bakti kebersihan masjid Sabtu ba'da Subuh")


def header(p, *, accent, text, muted, line, y0=34):
    """Identity band. Same in every direction — the mosque's name is not
    where a theme should express itself."""
    o = [txt(58, y0 + 52, "MASJID AL-FALAH", 52, text, weight=600, spacing=5.2)]
    o.append(txt(58, y0 + 84, "JAKARTA SELATAN", 21, muted, spacing=6.3))
    o.append(txt(W - 58, y0 + 46, "١٧ ربيع الأول ١٤٤٧", 30, accent,
                 font=ARAB, anchor="end"))
    o.append(txt(W - 58, y0 + 82, "SENIN, 31 AGUSTUS 2026", 20, muted,
                 anchor="end", spacing=4.4))
    o.append(rect(58, y0 + 108, W - 116, 2, line))
    return "".join(o)


def ticker(y, *, text, line):
    return (rect(58, y, W - 116, 2, line) +
            txt(58, y + 44, TICKER, 27, text))


# ---------------------------------------------------------------------------
# ARAH A — MIHRAB
# ---------------------------------------------------------------------------
def build_a():
    base, surf, accent = "#0B1220", "#16223A", "#C9A227"
    text, muted, line = "#F2EFE6", "#7E8AA3", "#22304C"
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
         f'viewBox="0 0 {W} {H}">']
    o.append("<defs>"
             '<linearGradient id="ph" x1="0" y1="0" x2=".5" y2="1">'
             '<stop offset="0" stop-color="#3A2A18"/>'
             '<stop offset=".55" stop-color="#0D1018"/>'
             '<stop offset="1" stop-color="#06080E"/></linearGradient>'
             '<radialGradient id="warm" cx=".22" cy=".26" r=".46">'
             '<stop offset="0" stop-color="#FFC478" stop-opacity=".42"/>'
             '<stop offset="1" stop-color="#FFC478" stop-opacity="0"/></radialGradient>'
             '<radialGradient id="glow" cx=".5" cy=".26" r=".62">'
             f'<stop offset="0" stop-color="{accent}" stop-opacity=".34"/>'
             f'<stop offset="1" stop-color="{accent}" stop-opacity="0"/></radialGradient>'
             "</defs>")
    # foto latar + scrim terukur
    o.append(rect(0, 0, W, H, "url(#ph)"))
    o.append(rect(0, 0, W, H, "url(#warm)"))
    o.append('<g opacity=".26" fill="#000">')
    for x, w, h in ((0, 300, 400), (360, 340, 470), (760, 300, 400),
                    (1120, 360, 520), (1540, 300, 400)):
        o.append(arch(x, H - h, w, h, "#000"))
    o.append("</g>")
    o.append(rect(0, 0, W, H, base, opacity=.62))

    o.append(header(None, accent=accent, text=text, muted=muted, line=line))

    # panggung: pengumuman
    o.append(txt(58, 246, "PENGUMUMAN", 19, accent, spacing=6.5))
    o.append(rect(58, 266, 1216, 2, line))
    for i, ln in enumerate(["Kajian rutin Ahad pagi bersama Ustaz",
                            "Abdul Latif, pukul 06.30 di ruang utama."]):
        o.append(txt(58, 336 + i * 66, ln, 56, text, weight=500))
    o.append(txt(58, 512, "2 / 4", 19, muted, font=MONO, spacing=2.6))

    # jam
    o.append(txt(W - 58, 396, "17:24", 196, text, weight=600, anchor="end"))
    o.append(txt(W - 58, 440, "WIB", 20, muted, anchor="end", spacing=6))

    # Arkade mihrab. Didudukkan rapat di atas teks berjalan: arkade adalah
    # lantai layar, dan lantai yang melayang meninggalkan pita kosong.
    ay, ah, gap = 744, 196, 14
    bw = (W - 116 - gap * 6) / 7
    for i, (lbl, ar, tm) in enumerate(PRAYERS):
        x = 58 + i * (bw + gap)
        nxt, past = i == 5, i < 5
        if nxt:
            bh = 252
            y = ay + ah - bh
            o.append(arch(x, y, bw, bh, "#FFFFFF", opacity=.075))
            o.append(f'<clipPath id="cn"><path d="M{x},{y+bh} L{x},{y+bh*.42} '
                     f'C{x},{y+bh*.13} {x+bw*.22},{y} {x+bw/2},{y} '
                     f'C{x+bw*.78},{y} {x+bw},{y+bh*.13} {x+bw},{y+bh*.42} '
                     f'L{x+bw},{y+bh} Z"/></clipPath>')
            o.append(f'<g clip-path="url(#cn)">'
                     f'{rect(x, y, bw, bh, "url(#glow)")}</g>')
            o.append(rect(x + bw * .18, y + 3, bw * .64, 2, accent, opacity=.7))
            cx = x + bw / 2
            o.append(txt(cx, y + 66, lbl, 23, accent, anchor="middle", spacing=5))
            o.append(txt(cx, y + 94, ar, 19, muted, font=ARAB, anchor="middle"))
            o.append(txt(cx, y + 156, tm, 50, text, weight=600, anchor="middle"))
            o.append(txt(cx, y + 202, "−00:27:52", 24, accent, font=MONO,
                         anchor="middle", spacing=1.4))
        else:
            fg = "#4E5A72" if past else text
            fgm = "#4E5A72" if past else muted
            o.append(arch(x, ay, bw, ah, "#FFFFFF",
                          opacity=.012 if past else .030))
            cx = x + bw / 2
            o.append(txt(cx, ay + 62, lbl, 23, fg, anchor="middle", spacing=4.6))
            o.append(txt(cx, ay + 90, ar, 19, fgm, font=ARAB, anchor="middle"))
            o.append(txt(cx, ay + 152, tm, 43, fg, weight=600, anchor="middle"))

    o.append(ticker(978, text=text, line=line))
    o.append("</svg>")
    return "".join(o)


# ---------------------------------------------------------------------------
# ARAH B — LENTERA (rel kiri)
# ---------------------------------------------------------------------------
def build_b():
    base, accent = "#101014", "#2FA37B"
    text, muted, line = "#ECEAE4", "#79798A", "#2A2A33"
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
         f'viewBox="0 0 {W} {H}">']
    o.append("<defs>"
             '<linearGradient id="ph" x1="0" y1="0" x2=".4" y2="1">'
             '<stop offset="0" stop-color="#0F2A24"/>'
             '<stop offset=".56" stop-color="#12131A"/>'
             '<stop offset="1" stop-color="#08080C"/></linearGradient>'
             '<radialGradient id="em" cx=".74" cy=".22" r=".5">'
             f'<stop offset="0" stop-color="{accent}" stop-opacity=".26"/>'
             f'<stop offset="1" stop-color="{accent}" stop-opacity="0"/></radialGradient>'
             '<linearGradient id="rg" x1="0" y1="0" x2="1" y2="0">'
             f'<stop offset="0" stop-color="{accent}" stop-opacity="0"/>'
             f'<stop offset="1" stop-color="{accent}" stop-opacity=".34"/>'
             "</linearGradient></defs>")
    o.append(rect(0, 0, W, H, "url(#ph)"))
    o.append(rect(0, 0, W, H, "url(#em)"))
    o.append('<g opacity=".22" fill="#000">'
             '<circle cx="1300" cy="640" r="250"/>'
             '<rect x="1288" y="300" width="24" height="240"/>'
             '<rect x="900" y="520" width="130" height="400" rx="65"/>'
             '<rect x="1580" y="520" width="130" height="400" rx="65"/></g>')
    o.append(rect(0, 0, W, H, base, opacity=.66))

    o.append(header(None, accent=accent, text=text, muted=muted, line=line))

    # ---- rel kiri: relung berbaring, yang berikutnya menyorong ke kanan ----
    ry, rh, rgap = 208, 96, 10
    for i, (lbl, ar, tm) in enumerate(PRAYERS):
        y = ry + i * (rh + rgap)
        nxt, past = i == 5, i < 5
        bw = 452 if nxt else 402
        rr = rh / 2
        d = (f"M58,{y} L{58+bw-rr},{y} A{rr},{rr} 0 0 1 {58+bw-rr},{y+rh} "
             f"L58,{y+rh} Z")
        if nxt:
            o.append(f'<path d="{d}" fill="#FFFFFF" opacity=".07"/>')
            o.append(f'<path d="{d}" fill="url(#rg)"/>')
            # Aksen mengikuti lengkung tutupnya, bukan batang lurus yang
            # memotongnya — batang lurus terbaca sebagai cacat pemotongan.
            o.append(f'<path d="M{58+bw-rr},{y} A{rr},{rr} 0 0 1 {58+bw-rr},'
                     f'{y+rh}" fill="none" stroke="{accent}" stroke-width="3"/>')
        else:
            o.append(f'<path d="{d}" fill="#FFFFFF" '
                     f'opacity="{".010" if past else ".026"}"/>')
        fg = "#4A4A58" if past else (accent if nxt else text)
        fgm = "#4A4A58" if past else muted
        o.append(txt(94, y + 44, lbl, 24, fg, spacing=4.8))
        o.append(txt(94, y + 72, ar, 17, fgm, font=ARAB))
        # Angka berhenti sebelum lengkung tutup mulai menyempit.
        tx = 58 + bw - (rr + 18 if nxt else 44)
        o.append(txt(tx, y + (50 if nxt else 60), tm, 46 if nxt else 41,
                     "#4A4A58" if past else text, weight=600, anchor="end"))
        if nxt:
            o.append(txt(tx, y + 80, "−00:27:52", 20, accent, font=MONO,
                         anchor="end"))

    # ---- panggung: papan jadwal ----
    sx = 566
    o.append(rect(sx, 200, 2, 700, line))
    o.append(rect(1420, 200, 2, 700, line))
    o.append(txt(sx + 44, 246, "JADWAL HARI INI", 18, accent, spacing=6.1))
    for i, (lbl, ar, tm) in enumerate(PRAYERS):
        y = 302 + i * 84
        past, now = i < 5, i == 5
        fg = "#4A4A58" if past else (accent if now else text)
        fgm = "#4A4A58" if past else muted
        if now:
            o.append(f'<circle cx="{sx+30}" cy="{y-10}" r="6" fill="{accent}"/>')
            o.append(f'<circle cx="{sx+30}" cy="{y-10}" r="12" fill="{accent}" '
                     f'opacity=".22"/>')
        o.append(txt(sx + 62, y, lbl.title(), 33, fg, spacing=3.6))
        o.append(txt(sx + 232, y, ar, 22, fgm, font=ARAB))
        o.append(txt(1380, y, tm, 39, fg, weight=600, anchor="end"))
        if i < 6:
            o.append(rect(sx + 30, y + 26, 1350 - sx - 30, 1, line))

    # ---- jam ----
    o.append(txt(W - 58, 400, "17:24", 170, text, weight=600, anchor="end"))
    o.append(txt(W - 58, 444, "WIB", 19, muted, anchor="end", spacing=5.7))
    o.append(rect(1462, 528, 400, 2, line))
    o.append(txt(W - 58, 570, "MENUJU MAGHRIB", 17, muted, anchor="end", spacing=5.1))
    o.append(txt(W - 58, 630, "00:27:52", 45, accent, font=MONO, anchor="end"))

    o.append(ticker(966, text=text, line=line))
    o.append("</svg>")
    return "".join(o)


# ---------------------------------------------------------------------------
# ARAH C — FAJAR (tema terang)
# ---------------------------------------------------------------------------
def build_c():
    base, accent = "#F4F2EC", "#2E3A87"
    text, muted, line = "#14161F", "#6B7186", "#DCD8CE"
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
         f'viewBox="0 0 {W} {H}">']
    o.append("<defs>"
             '<linearGradient id="ph" x1="0" y1="0" x2=".3" y2="1">'
             '<stop offset="0" stop-color="#FDF6E8"/>'
             '<stop offset=".62" stop-color="#EEF0F6"/>'
             '<stop offset="1" stop-color="#E4E6EE"/></linearGradient>'
             '<radialGradient id="sun" cx=".3" cy=".16" r=".55">'
             '<stop offset="0" stop-color="#FFECC8" stop-opacity=".95"/>'
             '<stop offset="1" stop-color="#FFECC8" stop-opacity="0"/></radialGradient>'
             '<radialGradient id="glow" cx=".5" cy=".24" r=".6">'
             f'<stop offset="0" stop-color="{accent}" stop-opacity=".16"/>'
             f'<stop offset="1" stop-color="{accent}" stop-opacity="0"/></radialGradient>'
             '<linearGradient id="im" x1="0" y1="0" x2=".7" y2="1">'
             '<stop offset="0" stop-color="#DFE7F7"/>'
             '<stop offset="1" stop-color="#C4D2EC"/></linearGradient>'
             "</defs>")
    o.append(rect(0, 0, W, H, "url(#ph)"))
    o.append(rect(0, 0, W, H, "url(#sun)"))
    o.append('<g opacity=".10">')
    for x, w, h in ((0, 280, 470), (340, 320, 520), (720, 280, 470),
                    (1060, 340, 550), (1460, 280, 470)):
        o.append(arch(x, H - h, w, h, accent))
    o.append("</g>")
    o.append(rect(0, 0, W, H, base, opacity=.50))

    o.append(header(None, accent=accent, text=text, muted=muted, line="#14161F"))

    # panggung: kartu slider
    cx0, cy0, cw, ch = 58, 218, 1216, 420
    o.append(rect(cx0, cy0, cw, ch, "#FFFFFF"))
    o.append(rect(cx0, cy0, cw, 344, "url(#im)"))
    o.append(f'<g opacity=".26" fill="{accent}">'
             f'<circle cx="{cx0+cw/2}" cy="{cy0+250}" r="106"/>'
             f'<rect x="{cx0+cw/2-7}" y="{cy0+52}" width="14" height="120"/>'
             f'<rect x="{cx0+230}" y="{cy0+150}" width="72" height="194" rx="36"/>'
             f'<rect x="{cx0+cw-302}" y="{cy0+150}" width="72" height="194" rx="36"/></g>')
    o.append(rect(cx0, cy0 + 344, cw, 2, line))
    o.append(txt(cx0 + 26, cy0 + 396, "Kegiatan Ramadan 1447 H", 26, text))
    o.append(txt(cx0 + cw - 26, cy0 + 396, "3 / 6", 18, muted, font=MONO,
                 anchor="end"))
    # indikator: batang, bukan titik — menunjukkan sisa waktu tayang
    tw = (cw - 8 * 5) / 6
    for i in range(6):
        x = cx0 + i * (tw + 8)
        o.append(rect(x, cy0 + ch + 16, tw, 4, "#B4BAC9" if i < 2 else line))
        if i == 2:
            o.append(rect(x, cy0 + ch + 16, tw * .62, 4, accent))

    o.append(txt(W - 58, 400, "10:41", 186, text, weight=600, anchor="end"))
    o.append(txt(W - 58, 444, "WIB", 20, muted, anchor="end", spacing=6))

    # arkade terang: relung digambar dengan garis, didudukkan di atas teks berjalan
    ay, ah, gap = 764, 184, 14
    bw = (W - 116 - gap * 6) / 7
    for i, (lbl, ar, tm) in enumerate(PRAYERS):
        x = 58 + i * (bw + gap)
        nxt, past = i == 3, i < 3
        bh = 236 if nxt else ah
        y = ay + ah - bh
        if nxt:
            o.append(arch(x, y, bw, bh, "#FFFFFF"))
            o.append(arch(x, y, bw, bh, "url(#glow)"))
        elif not past:
            o.append(arch(x, y, bw, bh, "#FFFFFF", opacity=.60))
        stroke = accent if nxt else line
        sw = 2
        sy = y + bh * .42
        d = (f"M{x},{y+bh} L{x},{sy} C{x},{y+bh*.126} {x+bw*.22},{y} {x+bw/2},{y} "
             f"C{x+bw*.78},{y} {x+bw},{y+bh*.126} {x+bw},{sy} L{x+bw},{y+bh}")
        o.append(f'<path d="{d}" fill="none" stroke="{stroke}" stroke-width="{sw}"/>')
        fg = "#A9AEBC" if past else (accent if nxt else text)
        fgm = "#A9AEBC" if past else muted
        cx = x + bw / 2
        o.append(txt(cx, y + 58, lbl, 22, fg, anchor="middle", spacing=4.4))
        o.append(txt(cx, y + 86, ar, 18, fgm, font=ARAB, anchor="middle"))
        o.append(txt(cx, y + 146, tm, 47 if nxt else 41,
                     "#A9AEBC" if past else text, weight=600, anchor="middle"))
        if nxt:
            o.append(txt(cx, y + 190, "−01:14:37", 23, accent, font=MONO,
                         anchor="middle"))

    o.append(ticker(986, text=text, line="#14161F"))
    o.append("</svg>")
    return "".join(o)


# ---------------------------------------------------------------------------
# LEMBAR D — BACKGROUND SLIDE DINAMIS
# ---------------------------------------------------------------------------
def build_d():
    base, accent = "#0B1220", "#C9A227"
    text, muted, line = "#F2EFE6", "#7E8AA3", "#22304C"
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
         f'viewBox="0 0 {W} {H}">']
    o.append("<defs>"
             '<linearGradient id="pb" x1="0" y1="0" x2=".6" y2="1">'
             '<stop offset="0" stop-color="#FFF3D0"/>'
             '<stop offset=".5" stop-color="#FFD79A"/>'
             '<stop offset="1" stop-color="#E8B46C"/></linearGradient>'
             '<linearGradient id="pm" x1="0" y1="0" x2=".6" y2="1">'
             '<stop offset="0" stop-color="#5B7FA8"/>'
             '<stop offset=".52" stop-color="#31465F"/>'
             '<stop offset="1" stop-color="#1D2836"/></linearGradient>'
             '<linearGradient id="pd" x1="0" y1="0" x2=".6" y2="1">'
             '<stop offset="0" stop-color="#1A2436"/>'
             '<stop offset=".55" stop-color="#0D1220"/>'
             '<stop offset="1" stop-color="#05070C"/></linearGradient>'
             '<linearGradient id="s1" x1="0" y1="0" x2=".6" y2="1">'
             '<stop offset="0" stop-color="#2A3A24"/>'
             '<stop offset="1" stop-color="#0F1712"/></linearGradient>'
             '<linearGradient id="s2" x1="0" y1="0" x2=".6" y2="1">'
             '<stop offset="0" stop-color="#4A2A2A"/>'
             '<stop offset="1" stop-color="#1A0F12"/></linearGradient>'
             "</defs>")
    o.append(rect(0, 0, W, H, base))
    o.append(txt(52, 66, "BACKGROUND SLIDE DINAMIS", 38, text, weight=600, spacing=3.8))
    o.append(txt(52, 100, "SCRIM TERUKUR PER-FOTO  ·  SILANG-PUDAR + KEN BURNS  ·  "
                          "KONTRAS TEKS DIJAGA ≥ 4.5:1", 19, muted, spacing=3))
    o.append(rect(52, 122, W - 104, 2, line))

    # --- tiga contoh scrim ---
    cards = [("FOTO TERANG — luminansi p95 ≈ 0.88", "url(#pb)", .76,
              "scrim 0.76 — langit cerah butuh peredupan paling kuat"),
             ("FOTO SEDANG — luminansi p95 ≈ 0.54", "url(#pm)", .58,
              "scrim 0.58 — interior masjid, tekstur masih terbaca"),
             ("FOTO GELAP — luminansi p95 ≈ 0.21", "url(#pd)", .30,
              "scrim 0.30 — foto malam tidak dihabisi jadi bidang hitam")]
    cw, gap = 588, 26
    for i, (cap, fill, alpha, note) in enumerate(cards):
        x = 52 + i * (cw + gap)
        y = 168
        o.append(txt(x, y, cap, 16, accent, font=MONO))
        o.append(rect(x, y + 16, cw, 260, fill))
        o.append(rect(x, y + 16, cw, 260, base, opacity=alpha))
        o.append(f'<rect x="{x}" y="{y+16}" width="{cw}" height="260" '
                 f'fill="none" stroke="{line}" stroke-width="2"/>')
        o.append(txt(x + 20, y + 58, "MASJID AL-FALAH", 18, text, spacing=2.9))
        o.append(txt(x + 20, y + 250, "17:24", 40, text, weight=600))
        o.append(txt(x + cw - 20, y + 250, "MAGHRIB 17:52", 15, accent,
                     anchor="end", spacing=2.7))
        o.append(txt(x, y + 306, note, 17, muted))

    # --- transisi silang ---
    ty = 528
    o.append(rect(52, ty, W - 104, 2, line))
    o.append(txt(52, ty + 42, "TRANSISI ANTAR-SLIDE", 25, text, weight=600, spacing=3.2))
    for i, ln in enumerate([
            "Silang-pudar 1200 ms dengan gerak lambat 6% (Ken Burns) — bukan geser. "
            "Geser menarik mata ke tepi layar; pudar dan zoom",
            "membuat latar tetap latar. Tiap foto tampil 45 detik: cukup lama agar "
            "tidak berkedip, cukup pendek agar panel tidak membakar satu citra."]):
        o.append(txt(52, ty + 76 + i * 26, ln, 17, muted))

    steps = [("0 dtk · foto 1 · zoom 1.00", 1.0, 0.0),
             ("44 dtk · zoom 1.055", 0.0, 0.0),
             ("+600 ms · silang 50/50", 0.5, 0.5),
             ("+1200 ms · foto 2", 0.0, 1.0),
             ("89 dtk · siklus ulang", 0.0, 1.0)]
    sw, sgap = 348, 20
    for i, (lab, a1, a2) in enumerate(steps):
        x = 52 + i * (sw + sgap)
        y = ty + 118
        o.append(f'<clipPath id="c{i}"><rect x="{x}" y="{y}" width="{sw}" '
                 f'height="146"/></clipPath>')
        o.append(f'<g clip-path="url(#c{i})">')
        z = 1.055 if i in (1, 4) else 1.0
        dx, dy = (sw * (z - 1) / 2), (146 * (z - 1) / 2)
        aa = 1.0 if i in (0, 1) else a1
        o.append(f'<rect x="{x-dx}" y="{y-dy}" width="{sw*z}" height="{146*z}" '
                 f'fill="url(#s1)" opacity="{aa}"/>')
        o.append(f'<rect x="{x-dx}" y="{y-dy}" width="{sw*z}" height="{146*z}" '
                 f'fill="url(#s2)" opacity="{a2}"/>')
        o.append("</g>")
        o.append(f'<rect x="{x}" y="{y}" width="{sw}" height="146" fill="none" '
                 f'stroke="{line}" stroke-width="2"/>')
        o.append(txt(x, y + 174, lab, 15, muted, font=MONO))

    # --- tiga aturan ---
    ry = 900
    o.append(rect(52, ry, W - 104, 2, line))
    rules = [("SCRIM DIHITUNG, BUKAN DITETAPKAN",
              ["Alpha diambil dari persentil ke-95 luminansi foto, bukan",
               "rata-rata — yang merusak keterbacaan adalah bercak paling",
               "terang. Scrim dinaikkan sampai kontras teks mencapai 4.5:1."]),
             ("SCRIM IKUT BERUBAH SAAT SLIDE BERGANTI",
              ["Alpha di-tween bersamaan dengan silang-pudar. Tanpa itu,",
               "pindah dari foto malam ke foto siang membuat jam",
               "menghilang selama satu detik penuh."]),
             ("BATAS DEKODE & CADANGAN",
              ["Bitmap dibatasi ukuran panel — foto 12 MP tidak muat di RAM",
               "TV box. Bila berkas hilang atau rusak, layar jatuh ke warna",
               "dasar tema; panel tidak pernah menampilkan bidang kosong."])]
    colw = (W - 104 - 68) / 3
    for i, (head, lines) in enumerate(rules):
        x = 52 + i * (colw + 34)
        o.append(txt(x, ry + 40, head, 15, accent, weight=600, spacing=3.9))
        for j, ln in enumerate(lines):
            o.append(txt(x, ry + 72 + j * 26, ln, 17, "#B9C0CF"))
    o.append("</svg>")
    return "".join(o)


for name, fn in (("a-mihrab", build_a), ("b-lentera", build_b),
                 ("c-fajar", build_c), ("d-background-slide", build_d)):
    path = os.path.join(OUT, name + ".svg")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(fn())
    print(f"{name}.svg  {os.path.getsize(path):,} bytes")
