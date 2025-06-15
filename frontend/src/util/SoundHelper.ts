export class SoundHelper {
  private static notification?: HTMLAudioElement;

  static async playMessageSound() {
    if (!SoundHelper.notification) {
      SoundHelper.notification = new Audio("/sound.mp3");
      SoundHelper.notification.volume = 0.25;
    }

    try {
      await SoundHelper.notification.play();
    } catch (e) {
      console.log((e as Error).message);
    }
  }
}
