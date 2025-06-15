export default class RequestOptions {
  private headers: [string, string][];
  private method?: string;
  private credentials?: "include";
  private body?: string;
  private formData?: FormData;

  constructor() {
    this.headers = [];
  }

  setMethod(method: "GET" | "POST" | "DELETE" | "PUT"): RequestOptions {
    this.method = method;
    return this;
  }

  setCredentials(credentials: "include"): RequestOptions {
    this.credentials = credentials;
    return this;
  }

  setBody(body: string): RequestOptions {
    this.body = body;
    return this;
  }

  setFormData(formData: FormData): RequestOptions {
    this.formData = formData;
    return this;
  }

  addHeader(headerName: string, headerValue: string): RequestOptions {
    this.headers.push([headerName, headerValue]);
    return this;
  }

  toRequestInit(): RequestInit {
    // Example:
    //
    // ['Autorization', 'Key']
    // ['Another-Header', 'Another-Value']
    const headersMatrix: string[][] = [];
    this.headers.forEach(([headerName, headerValue]) => {
      const headerArray: string[] = [];
      headerArray.push(headerName);
      headerArray.push(headerValue);
      headersMatrix.push(headerArray);
    });

    const requestJsonOptions: RequestInit = {
      // @ts-ignore
      headers: headersMatrix,
    };

    if (this.method) {
      requestJsonOptions.method = this.method;
    }

    if (this.credentials) {
      requestJsonOptions.credentials = this.credentials;
    }

    if (this.body) {
      requestJsonOptions.body = this.body;
    }

    if (this.formData) {
      requestJsonOptions.body = this.formData;
    }

    return requestJsonOptions;
  }
}
