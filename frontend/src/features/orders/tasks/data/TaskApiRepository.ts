import { APPLICATION_SERVER } from "../../../../constants";
import ApiHelper from "../../../../util/api/ApiHelper";
import RequestOptions from "../../../../util/api/RequestOptions";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { Task } from "../domain/Task";

export class TaskApiRepository {
  constructor(
    private readonly apiHelper: ApiHelper,
    private readonly userApiRepository: UserApiRepository
  ) {}

  async createTask(task: Task): Promise<Task> {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify(task));

    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/tasks/create`,
      requestOptions
    );
  }

  async updateTask(task: Task): Promise<Task> {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify(task));

    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/tasks/update`,
      requestOptions
    );
  }

  async deleteTask(taskId: number): Promise<Task> {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchDeleteJson(
      `${APPLICATION_SERVER}/api/tasks/delete/${taskId}`,
      requestOptions
    );
  }

  async getTasks(orderId: number, autoId?: number): Promise<Task[]> {
    const requestOptions = new RequestOptions();
    requestOptions.addHeader(
      "Authorization",
      this.userApiRepository.getAuthorizedData().accessToken
    );

    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/tasks/order_auto_tasks?orderId=${orderId}${
        autoId ? `&autoId=${autoId}` : ""
      }`,
      requestOptions
    );
  }
}
