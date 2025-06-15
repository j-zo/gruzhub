export class FilesUtils {
  static download(file: Blob, filename: string, type: string) {
    // @ts-ignore
    if (window.navigator.msSaveOrOpenBlob)
      // IE10+
      // @ts-ignore
      window.navigator.msSaveOrOpenBlob(file, filename);
    else {
      // Others
      const a = document.createElement("a");
      const url = URL.createObjectURL(file);

      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();

      setTimeout(function () {
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      }, 0);
    }
  }
}
