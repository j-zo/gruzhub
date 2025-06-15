import ChangeListener from './interfaces/ChangeListener';

export default class ChangeListenerList {
  private listeners: ChangeListener[] = [];

  addListener(listener: ChangeListener): void {
    this.listeners.push(listener);
  }

  removeListener(listener: ChangeListener): void {
    this.listeners.splice(this.listeners.indexOf(listener), 1);
  }

  notifyListenersAboutChange(): void {
    const listenersCopy = [...this.listeners];
    listenersCopy.forEach((listener) => {
      if (listener && listener.onChanged) {
        listener.onChanged();
      }
    });
  }
}
