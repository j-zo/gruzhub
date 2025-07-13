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

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/tasks/create`,
        this.userApiRepository,
      requestOptions
    );
  }

  async updateTask(task: Task): Promise<Task> {
    const requestOptions = new RequestOptions();
    requestOptions.setBody(JSON.stringify(task));

    return this.apiHelper.fetchPostJson(
      `${APPLICATION_SERVER}/api/tasks/update`,
        this.userApiRepository,
      requestOptions
    );
  }

  async deleteTask(taskId: number): Promise<Task> {

    return this.apiHelper.fetchDeleteJson(
      `${APPLICATION_SERVER}/api/tasks/delete/${taskId}`,
        this.userApiRepository
    );
  }

  async getTasks(orderId: number, transportId?: number): Promise<Task[]> {
    return this.apiHelper.fetchGetJson(
      `${APPLICATION_SERVER}/api/tasks/order_transport_tasks?orderId=${orderId}${
          transportId ? `&transportId=${transportId}` : ""
      }`,
        this.userApiRepository
    );
  }
}
