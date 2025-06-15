export default class Waiter {
  static wait = (milliseconds: number): Promise<void> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve();
      }, milliseconds);
    });
  };
}
