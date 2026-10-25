class HapticsService {
  private enabled: boolean = true;

  public setEnabled(enabled: boolean) {
    this.enabled = enabled;
  }

  private vibrate(pattern: number | number[]) {
    if (!this.enabled) return;
    if (typeof window !== 'undefined' && 'vibrate' in navigator) {
      try {
        navigator.vibrate(pattern);
      } catch {
        // Ignored if not permitted
      }
    }
  }

  public tokenPlaced() {
    this.vibrate(15);
  }

  public scoutUsed() {
    this.vibrate([20, 40, 20]);
  }

  public lockToggled() {
    this.vibrate([30, 30]);
  }

  public clashWon() {
    this.vibrate([20, 50, 40]);
  }

  public clashLost() {
    this.vibrate([60, 40, 80]);
  }

  public clashTied() {
    this.vibrate([25, 25]);
  }

  public gameOver() {
    this.vibrate([50, 50, 100, 50, 150]);
  }
}

export const haptics = new HapticsService();
