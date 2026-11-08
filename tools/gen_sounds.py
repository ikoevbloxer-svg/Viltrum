#!/usr/bin/env python3
"""Generates all synthesized sounds + converts the user's mp3s to ogg for the Viltrum mod."""
import numpy as np, subprocess, os, scipy.io.wavfile as wf

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/viltrum/sounds')
TMP = '/tmp/viltrum_snd'
os.makedirs(OUT, exist_ok=True); os.makedirs(TMP, exist_ok=True)
rng = np.random.default_rng(1337)

def T(sec): return np.arange(int(SR * sec)) / SR
def norm(x, peak=0.92): return (x / (np.max(np.abs(x)) + 1e-9) * peak).astype(np.float32)
def fade(x, a=0.004, b=0.02):
    x = x.copy(); na = int(SR * a); nb = int(SR * b)
    if na > 0: x[:na] *= np.linspace(0, 1, na)
    if nb > 0: x[-nb:] *= np.linspace(1, 0, nb)
    return x

def shaped_noise(n, spec, seed=None):
    """white noise shaped in the frequency domain; circular => perfectly loopable"""
    r = np.random.default_rng(seed) if seed is not None else rng
    w = r.standard_normal(n)
    F = np.fft.rfft(w); f = np.fft.rfftfreq(n, 1 / SR)
    F *= spec(f)
    return np.fft.irfft(F, n)

def bp(lo, hi, slope=1.0):
    def s(f):
        f = np.maximum(f, 1.0)
        hi_roll = 1 / (1 + (f / hi) ** (2 * slope))
        lo_roll = 1 / (1 + (lo / f) ** (2 * slope))
        return hi_roll * lo_roll
    return s

def lp(x, fc):
    a = np.exp(-2 * np.pi * fc / SR); y = np.zeros_like(x); s = 0.0
    for i, v in enumerate(x):
        s = (1 - a) * v + a * s; y[i] = s
    return y

def write_ogg(name, x, stereo=False):
    wav = os.path.join(TMP, name + '.wav')
    wf.write(wav, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5',
                    os.path.join(OUT, name + '.ogg')], check=True)
    print('  wrote', name + '.ogg', '%.2fs' % (len(x) / SR))

# ---------------------------------------------------------------- flight loops
def wind_loop(sec, lo, hi, whistle, depth, seed):
    n = int(SR * sec); t = np.arange(n) / SR
    body = shaped_noise(n, bp(lo, hi, 1.5), seed)
    body /= np.std(body)
    # slow periodic gusts (integer cycles => seamless)
    gust = 1 + depth * (0.5 * np.sin(2 * np.pi * 3 * t / sec) + 0.3 * np.sin(2 * np.pi * 7 * t / sec + 1.3)
                        + 0.2 * np.sin(2 * np.pi * 13 * t / sec + 0.4))
    out = body * gust
    # thin whistle component
    wh = np.sin(2 * np.pi * whistle * t + 2.5 * np.sin(2 * np.pi * 5 * t / sec)) * (0.5 + 0.5 * np.sin(2 * np.pi * 2 * t / sec))
    out += 0.12 * wh * np.std(out)
    # low rumble
    rum = shaped_noise(n, bp(25, 140, 2), seed + 1); rum /= np.std(rum)
    out += 0.55 * rum * (1 + 0.2 * np.sin(2 * np.pi * 2 * t / sec))
    return norm(out, 0.8)

def whoosh(sec, f0, f1, boom=True, seed=5):
    n = int(SR * sec); t = np.arange(n) / SR
    out = np.zeros(n)
    centers = np.geomspace(f0, f1, 14)
    for i, c in enumerate(centers):
        nz = shaped_noise(n, bp(c * 0.7, c * 1.4, 2), seed + i); nz /= np.std(nz)
        peak = sec * (0.15 + 0.6 * i / (len(centers) - 1))
        env = np.exp(-((t - peak) / (sec * 0.16)) ** 2)
        out += nz * env * (1.1 - i / len(centers) * 0.5)
    if boom:
        out += 1.4 * np.sin(2 * np.pi * (60 + 30 * np.exp(-t * 5)) * t) * np.exp(-t * 6)
    out *= np.minimum(1, t / 0.02) * np.minimum(1, (sec - t) / 0.12)
    return norm(out, 0.9)

# ---------------------------------------------------------------- impacts
def sonic_boom():
    sec = 2.4; n = int(SR * sec); t = np.arange(n) / SR
    click = np.zeros(n); click[:int(0.004 * SR)] = rng.standard_normal(int(0.004 * SR)) * 2.5
    ph = 2 * np.pi * np.cumsum(28 + 70 * np.exp(-t * 14)) / SR
    boom = np.sin(ph) * np.exp(-t * 2.6) * 2.2
    boom += 0.6 * np.sin(2 * ph) * np.exp(-t * 4.0)
    nz = shaped_noise(n, bp(80, 5200, 1.2), 11); nz /= np.std(nz)
    burst = nz * np.exp(-t * 6.5) * 1.5
    rumble = shaped_noise(n, bp(20, 180, 2), 12); rumble /= np.std(rumble)
    rumble *= np.exp(-t * 1.5) * 0.9
    crackle = (rng.random(n) < 0.0022).astype(float) * rng.standard_normal(n) * np.exp(-t * 2.2) * 3
    x = np.tanh(1.7 * (click + boom + burst + rumble + crackle))
    for d, g in ((0.09, 0.4), (0.21, 0.25), (0.37, 0.14)):
        k = int(d * SR); x[k:] += g * x[:-k] * 0.6
    return fade(norm(x, 0.95), 0.0005, 0.25)

def impact(sec, f0, f1, debris, seed, rumble_decay=3.0):
    n = int(SR * sec); t = np.arange(n) / SR
    ph = 2 * np.pi * np.cumsum(f1 + (f0 - f1) * np.exp(-t * 18)) / SR
    th = np.sin(ph) * np.exp(-t * rumble_decay) * 2.0
    nz = shaped_noise(n, bp(60, 3000, 1.5), seed); nz /= np.std(nz)
    crunch = nz * np.exp(-t * 11) * 1.3
    r = np.random.default_rng(seed)
    deb = (r.random(n) < debris).astype(float) * r.standard_normal(n) * np.exp(-t * 4.5) * 2.2
    deb = lp(deb, 4200)
    x = np.tanh(1.5 * (th + crunch + deb))
    return fade(norm(x, 0.95), 0.0005, 0.12)

def charge():
    sec = 1.8; n = int(SR * sec); t = np.arange(n) / SR
    f = 70 * np.exp(t / sec * np.log(900 / 70))
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(ph) + 0.5 * np.sin(2 * ph + 0.3 * np.sin(ph * 0.5)) + 0.25 * np.sin(3 * ph)
    trem = 0.75 + 0.25 * np.sin(2 * np.pi * (6 + 18 * t / sec) * t)
    nz = shaped_noise(n, bp(300, 4000, 1.5), 3); nz /= np.std(nz)
    env = (t / sec) ** 1.6
    x = (tone * trem * 0.7 + nz * 0.5 * env) * env
    x *= np.minimum(1, (sec - t) / 0.05)
    return norm(x, 0.85)

def suit_equip():
    sec = 0.75; n = int(SR * sec); t = np.arange(n) / SR
    out = np.zeros(n)
    for start, g in ((0.0, 1.0), (0.17, 0.8), (0.33, 0.55)):
        nz = shaped_noise(n, bp(900, 7000, 1.3), int(start * 1000) + 21); nz /= np.std(nz)
        e = np.clip(t - start, 0, None); out += nz * g * (np.exp(-e * 16) * (t >= start))
    out += 0.35 * np.sin(2 * np.pi * 2300 * t) * np.exp(-t * 9) * (t > 0.35)
    out += 0.25 * np.sin(2 * np.pi * 3100 * t) * np.exp(-(t - 0.4) * 11) * (t > 0.4)
    return fade(norm(out, 0.8), 0.002, 0.08)

def grab_sound():
    sec = 0.7; n = int(SR * sec); t = np.arange(n) / SR
    cloth = shaped_noise(n, bp(500, 4500, 1.4), 51); cloth /= np.std(cloth)
    cloth *= np.exp(-t * 14) * 0.9
    thud = np.sin(2 * np.pi * (95 + 40 * np.exp(-t * 30)) * t) * np.exp(-t * 11) * 1.6
    creak = shaped_noise(n, bp(180, 900, 2), 52); creak /= np.std(creak)
    creak *= (np.exp(-((t - 0.28) / 0.12) ** 2)) * 0.5
    x = np.tanh(1.4 * (cloth + thud + creak))
    return fade(norm(x, 0.9), 0.001, 0.08)

# ---------------------------------------------------------------- build
def main():
    print('synthesizing...')
    write_ogg('flight_loop', wind_loop(4.0, 160, 2600, 1150, 0.35, 100))
    write_ogg('flight_fast', wind_loop(3.0, 260, 9000, 2300, 0.30, 200))
    write_ogg('flight_start', whoosh(1.5, 220, 5200, True, 40))
    write_ogg('flight_boost', whoosh(1.1, 400, 8200, True, 80))
    write_ogg('sonic_boom', sonic_boom())
    write_ogg('landing', impact(1.1, 140, 45, 0.0018, 31))
    write_ogg('slam', impact(1.9, 110, 30, 0.003, 32, 2.2))
    write_ogg('charge', charge())
    write_ogg('suit_equip', suit_equip())
    write_ogg('grab', grab_sound())

    print('converting user sounds...')
    up = '/mnt/user-data/uploads/'
    def conv(src, name, trim=None, fade_out=0.15, stereo=False, gain_peak=0.95):
        wav = os.path.join(TMP, name + '_src.wav')
        args = ['ffmpeg', '-y', '-loglevel', 'error', '-i', up + src, '-ac', '2' if stereo else '1', '-ar', str(SR)]
        if trim: args += ['-t', str(trim)]
        subprocess.run(args + [wav], check=True)
        sr, x = wf.read(wav); x = x.astype(np.float32) / 32768
        x = x if x.ndim > 1 else x
        x = x / (np.max(np.abs(x)) + 1e-9) * gain_peak
        nb = int(SR * fade_out)
        if x.ndim == 1: x[-nb:] *= np.linspace(1, 0, nb)
        else: x[-nb:, :] *= np.linspace(1, 0, nb)[:, None]
        write_ogg(name, x)
    conv('heavy-punch-2.mp3', 'punch_heavy', trim=0.95, fade_out=0.2)
    conv('super-punch-1.mp3', 'punch_super', trim=2.6, fade_out=0.6)
    conv('barrage.mp3', 'punch_barrage', fade_out=0.15)
    conv('loud-bone-crack.mp3', 'bone_crack', fade_out=0.05)
    conv('laserbeam1612905990.mp3', 'laser', trim=1.8, fade_out=0.35)
    conv('levelupmusic_6ou5ZuG.mp3', 'level_up', fade_out=0.6, stereo=True)

main()
