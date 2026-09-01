"""ARAH E — FALAK. Layar sebagai instrumen, bukan tabel.

Waktu salat bukan daftar tujuh angka; waktu salat adalah posisi matahari.
Aplikasi ini sudah menghitung posisi itu untuk mendapatkan angkanya, lalu
membuangnya. Di sini posisi itu yang digambar, dan angka menggantung
darinya — jadi setiap tanda di busur berada pada sudut yang benar-benar
dihitung, bukan yang enak dipandang.

Semua geometri di bawah berasal dari satu fungsi ketinggian matahari yang
sama, sehingga gambar ini tidak bisa berbohong tentang jadwalnya sendiri.
"""
import math
import os

W, H = 1920, 1080
OUT = os.path.dirname(os.path.abspath(__file__))

# Jakarta Selatan, 31 Agustus. Deklinasi dan waktu istiwa dipakai apa adanya
# agar keenam tanda jatuh tepat pada jam yang dipakai di ketiga mockup lain.
LAT = math.radians(-6.21)
DEC = math.radians(8.09)
NOON = 11.8667

FUT = "Futura, Avenir Next, sans-serif"
COP = "Copperplate, Copperplate Gothic Light, serif"
# Diwan Kufi indah tapi lebar dan bertumpuk pada ukuran kecil; dipakai hanya
# untuk tanggal Hijriah yang berdiri sendiri.
KUF = "Geeza Pro, Baghdad, serif"

BASE = "#0E1A1E"       # tinta petrol — gelap yang condong hijau, bukan navy
NIGHT = "#081114"      # di bawah ufuk: benar-benar malam
TEXT = "#EAF0EE"       # kapur dingin
MUTED = "#6E8A8C"
LINE = "#22383C"


def alt(t):
    """Ketinggian matahari (derajat) pada jam desimal t."""
    ha = math.radians(15 * (t - NOON))
    s = (math.sin(LAT) * math.sin(DEC) +
         math.cos(LAT) * math.cos(DEC) * math.cos(ha))
    return math.degrees(math.asin(max(-1.0, min(1.0, s))))


# --- Aksen mengikuti matahari -------------------------------------------------
# Risiko yang diambil arah ini: warna aksen bukan konstanta tema, melainkan
# warna matahari pada ketinggiannya saat itu. Alasannya bukan puitis saja —
# layar ini menyala di serambi 24 jam. Aksen terang pukul 3 pagi menyakitkan
# mata; aksen redup saat tengah hari tidak terbaca.
STOPS = [(-90, "#8FAEC8"), (-12, "#8FAEC8"), (-6, "#B0574C"), (-1, "#E4602C"),
         (3, "#F58B3C"), (12, "#FFC46A"), (30, "#FFE3A8"), (60, "#FFF7E4"),
         (90, "#FFF7E4")]


def _mix(c1, c2, k):
    a = [int(c1[i:i + 2], 16) for i in (1, 3, 5)]
    b = [int(c2[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(a[i] + (b[i] - a[i]) * k):02X}" for i in range(3))


def sun_colour(a):
    for i in range(len(STOPS) - 1):
        a0, c0 = STOPS[i]
        a1, c1 = STOPS[i + 1]
        if a0 <= a <= a1:
            return _mix(c0, c1, 0 if a1 == a0 else (a - a0) / (a1 - a0))
    return STOPS[-1][1]


def _lum(c):
    def ch(v):
        v /= 255
        return v / 12.92 if v <= .03928 else ((v + .055) / 1.055) ** 2.4
    return (.2126 * ch(int(c[1:3], 16)) + .7152 * ch(int(c[3:5], 16)) +
            .0722 * ch(int(c[5:7], 16)))


def contrast(fg, bg):
    a, b = _lum(fg), _lum(bg)
    return (max(a, b) + .05) / (min(a, b) + .05)


# --- Bidang gambar ------------------------------------------------------------
CX0, CX1 = 58, 1862
T0, T1 = 3.9, 20.3
CY_TOP, CY_BOT = 330.0, 790.0
A_TOP, A_BOT = 80.0, -30.0
PXDEG = (CY_BOT - CY_TOP) / (A_TOP - A_BOT)


def px(t):
    return CX0 + (t - T0) * (CX1 - CX0) / (T1 - T0)


def py(a):
    return CY_TOP + (A_TOP - a) * PXDEG


UFUK = py(0)

# Imsak tidak diberi tanda sendiri: ia turunan dari Subuh, sepuluh menit
# sebelumnya, dan dua tanda berjarak 18 px hanya akan bertabrakan. Ia ikut
# menumpang di blok Subuh — struktur mengikuti isi.
MARKS = [
    ("SUBUH", "الفجر", 4.6333, "04:38", 0),
    ("TERBIT", "الشروق", 5.8833, "05:53", 1),
    ("DZUHUR", "الظهر", 11.9333, "11:56", 0),
    ("ASHAR", "العصر", 15.2333, "15:14", 1),
    ("MAGHRIB", "المغرب", 17.8667, "17:52", 0),
    ("ISYA", "العشاء", 19.0333, "19:02", 1),
]

TICKER = ("Infak pembangunan menara telah mencapai Rp 412.750.000 — terima kasih "
          "atas dukungan jamaah   ·   Kerja bakti kebersihan masjid Sabtu ba'da Subuh")


def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def txt(x, y, s, size, fill, *, font=FUT, weight=400, anchor="start",
        spacing=0, opacity=1):
    ls = f' letter-spacing="{spacing:.2f}"' if spacing else ""
    op = f' opacity="{opacity}"' if opacity != 1 else ""
    return (f'<text x="{x:.1f}" y="{y:.1f}" font-family="{font}" '
            f'font-size="{size}" font-weight="{weight}" fill="{fill}" '
            f'text-anchor="{anchor}"{ls}{op}>{esc(s)}</text>')


def rect(x, y, w, h, fill, *, opacity=1):
    op = f' opacity="{opacity}"' if opacity != 1 else ""
    return (f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" '
            f'fill="{fill}"{op}/>')


def hhmmss(sec):
    return f"{sec // 3600:02d}:{sec % 3600 // 60:02d}:{sec % 60:02d}"


def frame(now, *, uid, photo, scrim, nxt=4, k=1.0):
    """Satu bingkai penuh 1920x1080. `nxt` adalah indeks tanda berikutnya.

    `k` mengalikan tebal garis. Saat bingkai ini dikecilkan jadi gambar mini,
    garis 1 px menyusut jadi 0,3 px dan lenyap — bingkai jadi terlihat kosong
    padahal isinya lengkap. Pemanggil mengirim k = 1/skala.
    """
    a_now = alt(now)
    acc = sun_colour(a_now)
    o = []

    # --- latar: foto + scrim terukur ---
    o.append(f'<rect width="{W}" height="{H}" fill="url(#{photo})"/>')
    o.append(f'<g opacity=".30" fill="#000">'
             f'<circle cx="1420" cy="880" r="230"/>'
             f'<rect x="1408" y="560" width="22" height="220"/>'
             f'<rect x="1080" y="740" width="104" height="340" rx="52"/>'
             f'<rect x="1680" y="740" width="104" height="340" rx="52"/></g>')
    o.append(rect(0, 0, W, H, BASE, opacity=scrim))

    # --- kop ---
    o.append(txt(58, 88, "MASJID AL-FALAH", 50, TEXT, weight=500, spacing=4.4))
    o.append(txt(58, 118, "JAKARTA SELATAN", 17, MUTED, font=COP, weight=700,
                 spacing=5.4))
    o.append(txt(W - 58, 88, "١٧ ربيع الأول ١٤٤٧", 28, acc, font=KUF, anchor="end"))
    o.append(txt(W - 58, 118, "SENIN, 31 AGUSTUS 2026", 17, MUTED, font=COP,
                 weight=700, anchor="end", spacing=4.2))
    o.append(rect(58, 142, W - 116, 1 * k, LINE))

    # --- pita atas: hitung mundur memimpin, jam dinding mengikuti ---
    # Setiap jam masjid membesarkan jam dindingnya. Tugas layar ini bukan
    # memberi tahu pukul berapa sekarang, melainkan berapa lama lagi.
    label, ar, t_nxt, clock_nxt, _ = MARKS[nxt]
    left = max(0, int(round((t_nxt - now) * 3600)))
    o.append(txt(58, 210, f"MENUJU {label}", 19, acc, font=COP, weight=700,
                 spacing=6.2))
    o.append(txt(56, 300, hhmmss(left), 108, TEXT, weight=500, spacing=-1.5))
    o.append(txt(560, 300, f"· {clock_nxt}", 34, MUTED, weight=400))
    hh, mm = int(now), int(now % 1 * 60)
    o.append(txt(W - 58, 292, f"{hh:02d}:{mm:02d}", 96, TEXT, weight=400,
                 anchor="end", spacing=-1))
    o.append(txt(W - 58, 326, "WIB", 17, MUTED, font=COP, weight=700,
                 anchor="end", spacing=5.6))

    # --- bidang malam: di bawah ufuk memang malam ---
    o.append(rect(CX0, UFUK, CX1 - CX0, CY_BOT - UFUK, NIGHT, opacity=.55))

    # --- almukantarat: dua garis ketinggian saja ---
    for deg in (30, 60):
        y = py(deg)
        o.append(f'<line x1="{CX0}" y1="{y:.1f}" x2="{CX1}" y2="{y:.1f}" '
                 f'stroke="{LINE}" stroke-width="{1 * k:.2f}" '
                 f'stroke-dasharray="{2 * k:.1f} {9 * k:.1f}"/>')
        o.append(txt(CX0 + 6, y - 8, f"{deg}°", 14, MUTED, font=COP, weight=700,
                     spacing=1.6))

    # --- ufuk ---
    o.append(rect(CX0, UFUK, CX1 - CX0, 1 * k, MUTED, opacity=.72))
    o.append(txt(CX0 + 6, UFUK - 10, "UFUK 0°", 14, MUTED, font=COP, weight=700,
                 spacing=2.4))

    # --- busur matahari, dicuplik dari fungsi yang sama ---
    up, down = [], []
    t = T0
    while t <= T1 + 1e-9:
        a = alt(t)
        if a >= A_BOT:
            (up if a >= 0 else down).append(f"{px(t):.1f},{py(a):.1f}")
        t += 1 / 30
    o.append(f'<polyline points="{" ".join(down)}" fill="none" stroke="{MUTED}" '
             f'stroke-width="{2 * k:.2f}" opacity=".34" '
             f'stroke-dasharray="{5 * k:.1f} {7 * k:.1f}"/>')
    o.append(f'<polyline points="{" ".join(up)}" fill="none" stroke="{TEXT}" '
             f'stroke-width="{2.5 * k:.2f}" opacity=".62"/>')

    # --- kaliper: jarak tempuh matahari menuju waktu berikutnya ---
    # Hitung mundur muncul dua kali di layar ini, tapi tidak sebagai angka
    # kembar: di atas sebagai angka, di sini sebagai jarak yang terukur.
    seg = []
    t = min(now, t_nxt)
    while t <= max(now, t_nxt) + 1e-9:
        seg.append(f"{px(t):.1f},{py(alt(t)):.1f}")
        t += 1 / 120
    x0, x1 = px(now), px(t_nxt)
    y0, y1 = py(a_now), py(alt(t_nxt))
    # Ruas aksen ini pendek — 28 menit pada sumbu 16 jam hanya 51 px. Diberi
    # bidang arsir turun ke ufuk agar sisa waktu terbaca sebagai luas, bukan
    # sebagai goresan; inilah satu-satunya tempat layar ini bersuara keras.
    o.append(f'<path d="M{x0:.1f},{UFUK:.1f} L{" L".join(seg)} '
             f'L{x1:.1f},{UFUK:.1f} Z" fill="{acc}" opacity=".16"/>')
    o.append(f'<polyline points="{" ".join(seg)}" fill="none" stroke="{acc}" '
             f'stroke-width="{5 * k:.2f}" stroke-linecap="round"/>')
    for tt, yy in ((now, y0), (t_nxt, y1)):
        x = px(tt)
        o.append(f'<line x1="{x:.1f}" y1="{yy:.1f}" x2="{x:.1f}" '
                 f'y2="{UFUK:.1f}" stroke="{acc}" stroke-width="{2 * k:.2f}" '
                 f'opacity=".55"/>')

    # --- tanda waktu + garis penunjuk turun ke rel label ---
    for i, (lbl, arb, tt, clk, rail) in enumerate(MARKS):
        x, y = px(tt), py(alt(tt))
        past = tt < now
        nx = i == nxt
        fg = "#43585C" if past else (acc if nx else TEXT)
        fgm = "#3A4C50" if past else MUTED
        ny = 812 if rail == 0 else 906
        o.append(f'<line x1="{x:.1f}" y1="{y + 12:.1f}" x2="{x:.1f}" '
                 f'y2="{ny - 48:.1f}" stroke="{acc if nx else LINE}" '
                 f'stroke-width="{(2 if nx else 1) * k:.2f}" '
                 f'opacity="{1 if nx else .85}"/>')
        r = 7 if nx else 5
        o.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="{BASE}" '
                 f'stroke="{fg}" stroke-width="{(3 if nx else 2) * k:.2f}"/>')
        # Nama Arab duduk di atas nama Latin, keduanya rata tengah pada
        # penunjuk — bukan berdampingan, yang membuat keduanya bertabrakan.
        lx = max(CX0 + 92, min(CX1 - 92, x))
        o.append(txt(lx, ny - 26, arb, 20, fgm, font=KUF, anchor="middle"))
        o.append(txt(lx, ny, lbl, 26 if nx else 24, fg, weight=500,
                     anchor="middle", spacing=3.4))
        o.append(txt(lx, ny + 42, clk, 46 if nx else 41,
                     "#43585C" if past else TEXT, weight=500, anchor="middle"))
        if i == 0:
            # Imsak menumpang di blok Subuh; ditaruh di bawah agar tidak
            # menabrak blok Terbit yang duduk di rel bawah.
            o.append(txt(lx, ny + 70, "imsak 04:28", 17, fgm, anchor="middle"))

    # --- cakram matahari: isi bila di atas ufuk, kosong bila di bawah ---
    sx, sy = px(now), py(a_now)
    o.append(f'<circle cx="{sx:.1f}" cy="{sy:.1f}" r="46" fill="{acc}" '
             f'opacity=".13"/>')
    if a_now >= 0:
        o.append(f'<circle cx="{sx:.1f}" cy="{sy:.1f}" r="15" fill="{acc}"/>')
    else:
        o.append(f'<circle cx="{sx:.1f}" cy="{sy:.1f}" r="15" fill="{NIGHT}" '
                 f'stroke="{acc}" stroke-width="{3 * k:.2f}"/>')
    o.append(txt(sx + 30, sy - 14, f"{a_now:+.1f}°", 22, acc, weight=500))

    # --- teks berjalan ---
    o.append(rect(58, 972, W - 116, 1 * k, LINE))
    o.append(txt(58, 1016, TICKER, 26, TEXT))
    return "".join(o)


PHOTOS = {
    "senja": ('<linearGradient id="senja" x1="0" y1="0" x2=".45" y2="1">'
              '<stop offset="0" stop-color="#2E1E16"/>'
              '<stop offset=".5" stop-color="#132025"/>'
              '<stop offset="1" stop-color="#070C0F"/></linearGradient>'),
    "siang": ('<linearGradient id="siang" x1="0" y1="0" x2=".45" y2="1">'
              '<stop offset="0" stop-color="#CFE2F2"/>'
              '<stop offset=".55" stop-color="#8FB0C4"/>'
              '<stop offset="1" stop-color="#5E7A88"/></linearGradient>'),
    "malam": ('<linearGradient id="malam" x1="0" y1="0" x2=".45" y2="1">'
              '<stop offset="0" stop-color="#16222E"/>'
              '<stop offset=".55" stop-color="#0A1218"/>'
              '<stop offset="1" stop-color="#04070A"/></linearGradient>'),
}


def wrap(body, defs=""):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
            f'viewBox="0 0 {W} {H}"><defs>{"".join(PHOTOS.values())}{defs}</defs>'
            f'<rect width="{W}" height="{H}" fill="{BASE}"/>{body}</svg>')


def build_main():
    return wrap(frame(17.4, uid="m", photo="senja", scrim=.58, nxt=4))


def build_sheet():
    """Lembar perilaku: aksen dan scrim sepanjang hari, di atas slide berganti."""
    o = [rect(0, 0, W, H, BASE)]
    o.append(txt(52, 74, "SATU ARAH, SEPANJANG HARI", 40, TEXT, weight=500,
                 spacing=3))
    o.append(txt(52, 108, "WARNA AKSEN DIAMBIL DARI KETINGGIAN MATAHARI  ·  "
                          "SCRIM DIUKUR DARI TIAP FOTO", 17, MUTED, font=COP,
                 weight=700, spacing=3.4))
    o.append(rect(52, 130, W - 104, 1, LINE))

    # Jam dipilih agar tiap bingkai masih punya waktu berikutnya hari itu —
    # sesudah Isya, "berikutnya" adalah Subuh besok dan busurnya bukan busur
    # hari ini lagi. Itu kasus tersendiri, bukan bahan lembar warna.
    shots = [(11.35, "siang", .78, "TENGAH HARI",
              "Aksen nyaris putih-emas. Foto langit terang menuntut scrim 0.78."),
             (17.40, "senja", .58, "PETANG",
              "Aksen bara. Interior masjid, tekstur foto masih terbaca di 0.58."),
             (18.60, "malam", .28, "SESUDAH MAGHRIB",
              "Aksen perak dingin. Foto malam tidak perlu dihabisi — scrim 0.28.")]
    fw, fgap = 588, 26
    s = fw / W
    for i, (t, ph, sc, cap, note) in enumerate(shots):
        x = 52 + i * (fw + fgap)
        y = 178
        nxt = 2 if t < 12 else (4 if t < 17.87 else 5)
        cap = f"{cap}  ·  {alt(t):+.0f}°"
        o.append(f'<clipPath id="k{i}"><rect x="{x}" y="{y}" width="{fw}" '
                 f'height="{H * s:.1f}"/></clipPath>')
        # Klip harus duduk di induk yang tidak ditransformasi: bila klip dan
        # isinya berbagi satu transform, jendela klip ikut mengecil dan
        # gambar mini terpotong jadi sepertiga ukurannya.
        o.append(f'<g clip-path="url(#k{i})"><g transform="translate({x},{y}) '
                 f'scale({s:.5f})">'
                 f'{frame(t, uid=f"s{i}", photo=ph, scrim=sc, nxt=nxt, k=1 / s)}'
                 f'</g></g>')
        o.append(f'<rect x="{x}" y="{y}" width="{fw}" height="{H * s:.1f}" '
                 f'fill="none" stroke="{LINE}" stroke-width="1"/>')
        o.append(txt(x, y + H * s + 38, cap, 17, sun_colour(alt(t)), font=COP,
                     weight=700, spacing=3.2))
        o.append(txt(x, y + H * s + 68, note, 17, MUTED))

    # --- pita warna aksen sepanjang 24 jam ---
    by = 596
    o.append(rect(52, by, W - 104, 1, LINE))
    o.append(txt(52, by + 42, "WARNA AKSEN, 00:00 → 24:00", 24, TEXT, weight=500,
                 spacing=2.6))
    bw = (W - 104) / 192
    for i in range(192):
        t = i * 24 / 192
        o.append(rect(52 + i * bw, by + 64, bw + .6, 56, sun_colour(alt(t))))
    for hh in (0, 6, 12, 18, 24):
        x = 52 + (W - 104) * hh / 24
        o.append(txt(min(x, W - 66), by + 142, f"{hh:02d}", 15, MUTED, font=COP,
                     weight=700, anchor="middle" if 0 < hh < 24 else
                     ("start" if hh == 0 else "end")))

    # --- aturan ---
    ry = 770
    o.append(rect(52, ry, W - 104, 1, LINE))
    rules = [("SUDUT DIHITUNG, BUKAN DIGAMBAR",
              ["Tiap tanda duduk pada ketinggian matahari yang sebenarnya.",
               "Subuh −19,1°, Ashar +37,7°, Maghrib −0,9°. Gambar ini tidak",
               "bisa berbohong tentang jadwalnya sendiri."]),
             ("HITUNG MUNDUR MEMIMPIN, JAM MENGIKUTI",
              ["Tugas layar serambi bukan memberi tahu pukul berapa sekarang,",
               "melainkan berapa lama lagi. Angka besar adalah sisa waktu;",
               "jam dinding tetap terbaca, tapi tidak lagi mendominasi."]),
             ("AKSEN IKUT MATAHARI, SCRIM IKUT FOTO",
              ["Aksen terang pukul tiga pagi menyakitkan mata di serambi",
               "gelap; aksen redup tengah hari tidak terbaca. Keduanya",
               "di-tween bersama silang-pudar slide, 1200 ms."])]
    colw = (W - 104 - 68) / 3
    for i, (head, lines) in enumerate(rules):
        x = 52 + i * (colw + 34)
        o.append(txt(x, ry + 44, head, 16, sun_colour(alt(17.4)), font=COP,
                     weight=700, spacing=3.4))
        for j, ln in enumerate(lines):
            o.append(txt(x, ry + 78 + j * 27, ln, 17, "#A8BCBC"))
    return wrap("".join(o))


for name, fn in (("e-falak", build_main), ("e-falak-lembar", build_sheet)):
    p = os.path.join(OUT, name + ".svg")
    with open(p, "w", encoding="utf-8") as fh:
        fh.write(fn())
    print(f"{name}.svg  {os.path.getsize(p):,} bytes")

print()
for who, c in (("teks", TEXT), ("redup", MUTED), ("aksen petang", sun_colour(alt(17.4))),
               ("aksen malam", sun_colour(alt(20.67))), ("aksen siang", sun_colour(alt(11.35)))):
    print(f"  kontras {who:14} atas dasar  {contrast(c, BASE):5.2f}:1")
