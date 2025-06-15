export class TextCopier {
  copyTokenToClipboard(text: string) {
    const dummy = document.createElement('textarea');

    document.body.appendChild(dummy);

    dummy.value = text;
    dummy.select();
    document.execCommand('copy');
    document.body.removeChild(dummy);
  }
}
