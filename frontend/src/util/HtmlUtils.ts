export class HtmlUtils {
  static isElementInViewport(elementId: string): boolean {
    const element = document.getElementById(elementId);
    if (!element) return false;

    const rect = element.getBoundingClientRect();

    return (
      rect.top >= 0 &&
      rect.left >= 0 &&
      rect.bottom <=
        (window.innerHeight || document.documentElement.clientHeight) &&
      rect.right <= (window.innerWidth || document.documentElement.clientWidth)
    );
  }
}
