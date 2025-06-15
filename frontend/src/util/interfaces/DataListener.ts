export default interface DataListener<T> {
  onDataReceived(data: T): void;
}
