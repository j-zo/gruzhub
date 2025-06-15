export default class UrlUtils {
  public static getValueAfterSlash(url: string): string | undefined {
    const urlWithoutLastSlash = url[url.length - 1] === '/' ? url.slice(0, url.length - 1) : url;
    const dataAfterSlash = urlWithoutLastSlash.split('/').pop() || '';
    const dataBeforeQuestion = dataAfterSlash.split('?')[0];

    return dataBeforeQuestion;
  }
}
