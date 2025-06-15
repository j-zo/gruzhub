// eslint-disable-next-line import/no-cycle
import UserApiRepository from "../../features/user/data/UserApiRepository";

import Waiter from "../Waiter";
import RequestOptions from "./RequestOptions";

/**
 * This class makes requests to external services and handles
 * some possible errors that may happen related to users and Telegram
 *
 * In case of network or Nginx error (that may happen on API reload):
 *
 * 1. We try to repeat request 10 times with interval of 3 seconds. This
 * is enough to give API time to restart
 *
 * 2. If error on production - we try to close Telegram App to avoid
 * further requests that will load our application
 *
 * In case on NOT_AUTHORIZED error, we try try clean access_token
 * in UserApiRepository and force reload the window. This error
 * usually means that password has been changed and user should
 * log out
 */
export default class ApiHelper {
  private readonly REPEAT_TRIES_COUNT = 10;
  private readonly REPEAT_INTERVAL_MS = 3_000;

  private userApiRepository?: UserApiRepository;

  attachUserRepository(userApiRepository: UserApiRepository) {
    this.userApiRepository = userApiRepository;
  }

  async fetchPostJson<T>(
    url: string,
    requestOptions?: RequestOptions,
    isSkipAuthError = false
  ): Promise<T> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .setMethod("POST")
      .addHeader("Content-Type", "application/json")
      .addHeader("Access-Control-Allow-Methods", "POST")
      .addHeader("Accept", "application/json");

    const response = await this.makeRequest(
      url,
      optionsWrapper,
      0,
      isSkipAuthError
    );

    return response.json();
  }

  async fetchPostRaw(
    url: string,
    requestOptions?: RequestOptions
  ): Promise<string> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .setMethod("POST")
      .addHeader("Content-Type", "application/json")
      .addHeader("Access-Control-Allow-Methods", "POST")
      .addHeader("Accept", "application/json");

    const response = await this.makeRequest(url, optionsWrapper);
    return response.text();
  }

  async fetchGetJson<T>(
    url: string,
    requestOptions?: RequestOptions
  ): Promise<T> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .addHeader("Content-Type", "application/json")
      .addHeader("Access-Control-Allow-Methods", "GET")
      .addHeader("Accept", "application/json");

    const response = await this.makeRequest(url, optionsWrapper);
    return response.json();
  }

  async fetchGetRaw(
    url: string,
    requestOptions?: RequestOptions
  ): Promise<void> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .addHeader("Content-Type", "application/json")
      .addHeader("Access-Control-Allow-Methods", "GET")
      .addHeader("Accept", "application/json");

    await this.makeRequest(url, optionsWrapper);
  }

  async fetchPutJson<T>(
    url: string,
    requestOptions?: RequestOptions
  ): Promise<T> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .setMethod("PUT")
      .addHeader("Content-Type", "application/json")
      .addHeader("Access-Control-Allow-Methods", "PUT")
      .addHeader("Accept", "application/json");

    const response = await this.makeRequest(url, optionsWrapper);
    return response.json();
  }

  async fetchDeleteJson<T>(
    url: string,
    requestOptions?: RequestOptions
  ): Promise<T> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .setMethod("DELETE")
      .addHeader("Access-Control-Allow-Methods", "DELETE")
      .addHeader("Accept", "application/json");

    const response = await this.makeRequest(url, optionsWrapper);
    return response.json();
  }

  async fetchDeleteRaw(
    url: string,
    requestOptions?: RequestOptions
  ): Promise<string> {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .setMethod("DELETE")
      .addHeader("Access-Control-Allow-Methods", "DELETE")
      .addHeader("Accept", "application/json");

    const response = await this.makeRequest(url, optionsWrapper);
    return response.text();
  }

  async fetchPostMultipart(url: string, requestOptions?: RequestOptions) {
    const optionsWrapper = requestOptions || new RequestOptions();

    optionsWrapper
      .setMethod("POST")
      .addHeader("Access-Control-Allow-Methods", "POST")
      .addHeader("Accept", "application/json");

    await this.makeRequest(url, optionsWrapper, this.REPEAT_TRIES_COUNT);
  }

  private async makeRequest(
    url: string,
    optionsWrapper: RequestOptions,
    currentTry = 0,
    isSkipAuthError = false
  ): Promise<Response> {
    try {
      const response = await fetch(url, optionsWrapper?.toRequestInit());
      await this.handleOrThrowMessageIfResponseError(response, isSkipAuthError);
      return response;
    } catch (e) {
      if (
        (e as Error).message.toLocaleLowerCase().includes("failed to fetch")
      ) {
        if (currentTry === this.REPEAT_TRIES_COUNT) {
          throw e;
        } else {
          await Waiter.wait(this.REPEAT_INTERVAL_MS);
          return this.makeRequest(url, optionsWrapper, currentTry + 1);
        }
      } else {
        throw e;
      }
    }
  }

  private async handleOrThrowMessageIfResponseError(
    response: Response,
    isSkipAuthError = false
  ) {
    if (!isSkipAuthError) {
      if (response.status === /* unauthorized */ 401) {
        if (this.userApiRepository) {
          this.userApiRepository.logout();
          window.location.reload();
        }
      }
    }

    // Nginx errors that may happen on server reload
    if (response.status === 502 || response.status === 504) {
      throw new Error("failed to fetch");
    }

    if (response.status >= 400 && response.status <= 600) {
      let jsonResponse: { message: string };
      let errorMessage: string | undefined;

      try {
        jsonResponse = await response.json();
        if (jsonResponse.message) {
          errorMessage = jsonResponse.message;
        }
      } catch (e) {
        // expected that .json() cannot be executed
      }

      if (errorMessage) {
        throw new Error(errorMessage);
      } else {
        throw new Error(await response.text());
      }
    }
  }
}
