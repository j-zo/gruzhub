import { APPLICATION_SERVER } from "../../../constants";
import ApiHelper from "../../../util/api/ApiHelper";
import RequestOptions from "../../../util/api/RequestOptions";
import ChangeListenerList from "../../../util/ChangeListenerList";
import ChangeListener from "../../../util/interfaces/ChangeListener";
import { CreateUser } from "../domain/CreateUser";
import SignInResponse from "../domain/SignInResponse";
import { UpdateUser } from "../domain/UpdateUser";
import { User } from "../domain/User";
import { UserRole } from "../domain/UserRole";

export default class UserApiRepository {
  private changeListeners: ChangeListenerList;

  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly authorizedAccessTokenKey: string,
    private readonly authorizedUserIdKey: string
  ) {
    this.changeListeners = new ChangeListenerList();
  }

  async signUp(createUser: CreateUser) {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify(createUser));

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/users/signup`,
        this,
      requestOptions
    );
  }

  async updateUser(updateUser: UpdateUser) {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify(updateUser));

    return this.apiHelper
      .fetchPostRaw(`${APPLICATION_SERVER}/api/users/update`, this, requestOptions)
      .then(() => {
        if (updateUser.password) {
          return this.signIn(
            updateUser.email,
            updateUser.phone,
            updateUser.password,
            updateUser.role
          );
        }
      })
      .then(() => this.changeListeners.notifyListenersAboutChange());
  }

  async getUsers(regionsIds?: number[], userRoles?: string[]): Promise<User[]> {
    const requestOptions: RequestOptions = new RequestOptions();

    requestOptions.setBody(
      JSON.stringify({
        roles: userRoles,
        regionsIds: regionsIds,
      })
    );

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/users/users`,
        this,
      requestOptions
    );
  }

  async signIn(
    email: string | undefined,
    phone: string | undefined,
    password: string,
    role: UserRole
  ): Promise<SignInResponse> {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify({ email, phone, password, role }));

    return this.apiHelper
      .fetchPostJson(
        `${APPLICATION_SERVER}/api/users/signin`,
          this,
        requestOptions,
        true
      )
      .then((response: unknown): SignInResponse => {
        const typedResponse = response as SignInResponse;
        this.saveAuthorizedData(typedResponse.accessToken, typedResponse.id);
        this.notifyAuthListeners();
        return typedResponse;
      });
  }

  async getUserById(userId: number): Promise<User> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/users/${userId}`,
      this
    );
  }

  async sendResetEmail(email: string, role: UserRole): Promise<void> {
    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/users/reset-code?email=${email}&role=${role}`,
        this
    );
  }

  async resetPassword(
    email: string,
    code: string,
    password: string,
    role: UserRole
  ): Promise<void> {
    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/users/reset-password?email=${email}&code=${code}&password=${password}&role=${role}`,
        this
    );
  }

  async connectTelegramViaTgAuth(queryParams: string) {
    const requestOptions: RequestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify({ queryParams }));

    return this.apiHelper.fetchPostRaw(
      `${APPLICATION_SERVER}/api/users/connect-telegram-via-tg-auth`,
        this,
      requestOptions
    );
  }

  async connectTelegramViaWebapp(tgId: number) {
    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/users/connect-telegram-via-webapp?tgId=${tgId}`,
      this
    );
  }

  async getUserAccess(userId: number): Promise<SignInResponse> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/users/get-access/${userId}`,
      this
    );
  }

  async connectTelegramChat(chatUuid: string) {

    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/users/connect-chat?chatUuid=${chatUuid}`,
      this
    );
  }

  async disconnectTelegramChat(chatUuid: string) {
    return this.apiHelper.fetchGetRaw(
      `${APPLICATION_SERVER}/api/users/disconnect-chat?chatUuid=${chatUuid}`,
      this
    );
  }

  isAuthorized(): boolean {
    return !!localStorage.getItem(this.authorizedAccessTokenKey);
  }

  getAuthorizedData(): { accessToken: string; authorizedUserId: number } {
    if (!this.isAuthorized()) throw new Error("Is not authorized");
    return {
      accessToken: localStorage.getItem(this.authorizedAccessTokenKey) || "",
      authorizedUserId: Number(
        localStorage.getItem(this.authorizedUserIdKey) || ""
      ),
    };
  }

  logout(): void {
    localStorage.removeItem(this.authorizedAccessTokenKey);
    localStorage.removeItem(this.authorizedUserIdKey);
  }

  // listeners

  addAuthListener = (listener: ChangeListener): void => {
    this.changeListeners.addListener(listener);
  };

  removeAuthListener = (listener: ChangeListener): void => {
    this.changeListeners.removeListener(listener);
  };

  notifyAuthListeners = (): void => {
    this.changeListeners.notifyListenersAboutChange();
  };

  saveAuthorizedData(accessToken: string, userId: number): void {
    localStorage.setItem(this.authorizedAccessTokenKey, accessToken);
    localStorage.setItem(this.authorizedUserIdKey, `${userId}`);
  }
}
