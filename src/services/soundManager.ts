class SoundManager {
  private ctx: AudioContext | null = null;
  private enabled: boolean = true;
  private volume: number = 0.8;

  constructor() {
    // AudioContext will be initialized upon first user interaction
  }

  private getContext(): AudioContext | null {
    if (!this.ctx && typeof window !== 'undefined') {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (AudioCtx) {
        this.ctx = new AudioCtx();
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
    return this.ctx;
  }

  public setEnabled(enabled: boolean) {
    this.enabled = enabled;
  }

  public setVolume(volume: number) {
    this.volume = Math.max(0, Math.min(1, volume));
  }

  private playTone(freq: number, type: OscillatorType, duration: number, gainVal: number = 0.2, attack: number = 0.01) {
    if (!this.enabled) return;
    const ctx = this.getContext();
    if (!ctx) return;

    try {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = type;
      osc.frequency.setValueAtTime(freq, ctx.currentTime);

      const effectiveGain = gainVal * this.volume;
      gain.gain.setValueAtTime(0, ctx.currentTime);
      gain.gain.linearRampToValueAtTime(effectiveGain, ctx.currentTime + attack);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + duration);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start();
      osc.stop(ctx.currentTime + duration);
    } catch {
      // Audio might be blocked by browser policy
    }
  }

  public playTokenPlace() {
    this.playTone(520, 'sine', 0.1, 0.25);
    setTimeout(() => this.playTone(660, 'sine', 0.08, 0.2), 30);
  }

  public playScout() {
    this.playTone(440, 'triangle', 0.12, 0.2);
    setTimeout(() => this.playTone(880, 'sine', 0.2, 0.3), 80);
  }

  public playLock() {
    this.playTone(300, 'square', 0.08, 0.15);
    setTimeout(() => this.playTone(450, 'triangle', 0.15, 0.25), 60);
  }

  public playClashWin() {
    this.playTone(523.25, 'triangle', 0.15, 0.3); // C5
    setTimeout(() => this.playTone(659.25, 'triangle', 0.18, 0.35), 100); // E5
    setTimeout(() => this.playTone(783.99, 'sine', 0.3, 0.4), 220); // G5
  }

  public playClashLose() {
    this.playTone(400, 'sawtooth', 0.15, 0.2);
    setTimeout(() => this.playTone(280, 'sawtooth', 0.25, 0.25), 120);
  }

  public playClashTie() {
    this.playTone(350, 'triangle', 0.15, 0.2);
    setTimeout(() => this.playTone(350, 'triangle', 0.15, 0.2), 120);
  }

  public playTimerTickSoft() {
    this.playTone(800, 'sine', 0.03, 0.08);
  }

  public playTimerTickUrgent() {
    this.playTone(1100, 'square', 0.06, 0.18);
  }

  public playTimerExpired() {
    this.playTone(220, 'sawtooth', 0.3, 0.25);
  }

  public playGameOverWin() {
    const notes = [523.25, 659.25, 783.99, 1046.5];
    notes.forEach((f, idx) => {
      setTimeout(() => this.playTone(f, 'sine', 0.4, 0.35), idx * 120);
    });
  }

  public playGameOverLose() {
    const notes = [440, 370, 311, 261];
    notes.forEach((f, idx) => {
      setTimeout(() => this.playTone(f, 'sawtooth', 0.35, 0.25), idx * 150);
    });
  }

  public playButtonClick() {
    this.playTone(600, 'sine', 0.04, 0.15);
  }

  public playLogoAppear() {
    this.playTone(330, 'triangle', 0.2, 0.2);
    setTimeout(() => this.playTone(440, 'triangle', 0.3, 0.3), 150);
    setTimeout(() => this.playTone(660, 'sine', 0.5, 0.35), 350);
  }
}

export const soundManager = new SoundManager();
