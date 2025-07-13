import { useEffect, useState } from "react";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { User } from "../../../user/domain/User";
import { Order } from "../../orders/domain/Order";
import { TaskApiRepository } from "@/features/orders/tasks/data/TaskApiRepository";
import { Task } from "../domain/Task";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import { Button } from "@mantine/core";
import { UserRole } from "../../../user/domain/UserRole";
import { TaskDialogComponent } from "./TaskDialogComponent";
import { TransportType } from "@/features/common/transport/domain/TransportType";
import Image from "next/image";

interface Props {
  taskApiRepository: TaskApiRepository;

  user: User;
  order: Order;
}

export const OrderTasksComponent = ({
  taskApiRepository,
  user,
  order,
}: Props): JSX.Element => {
  const [isLoading, setLoading] = useState(true);

  const [tasks, setTasks] = useState<Task[]>([]);

  const [isAddTask, setAddTask] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | undefined>();

  const deleteTask = async (task: Task) => {
    setLoading(true);

    try {
      await taskApiRepository.deleteTask(task.id);
      loadTasks();
    } catch (e) {
      alert("Не вышло удалить работу. Пожалуйста, перезагрузите страницу");
    }

    setLoading(false);
  };

  const editTask = (task: Task) => {
    setEditingTask(task);
  };

  const loadTasks = async (isSilent = false) => {
    if (!isSilent) {
      setLoading(true);
    }

    try {
      const tasks = await taskApiRepository.getTasks(order.id);
      setTasks(tasks);
    } catch (e) {
      alert(
        "Не вышло загрузить ремонтные работы. Пожалуйста, перезагрузите страницу"
      );
    }

    setLoading(false);
  };

  useEffect(() => {
    loadTasks();

    let updateTasksInterval: NodeJS.Timeout | undefined;
    let isRequestInProgress = false;

    if (user.role !== UserRole.MASTER) {
      updateTasksInterval = setInterval(async () => {
        if (!isRequestInProgress) {
          isRequestInProgress = true;
          await loadTasks(/* isSilent */ true);
          isRequestInProgress = false;
        }
      }, 10_000);
    }
    return () => {
      if (updateTasksInterval) clearInterval(updateTasksInterval);
    };
  }, []);

  return (
    <div>
      {isLoading ? (
        <div className="my-3">
          <LoadingComponent size="xs" />
        </div>
      ) : (
        <>
          <div className="font-bold mb-3">Ремонтные работы</div>

          <div className="mb-3">
            {tasks?.length === 0 ? (
              <div className="text-gray-600 text-sm">Нет ремонтных работ</div>
            ) : (
              <div className=" max-w-full overflow-x-auto">
                <table className="table-auto text-sm">
                  <thead>
                    <tr>
                      <th className="min-w-[85px]">ТС</th>
                      <th className="min-w-[200px]">Наименование работы</th>
                      <th className="min-w-[200px]">Описание</th>
                      <th className="min-w-[100px]">Стоимость</th>
                      {user.role === UserRole.MASTER && (
                        <th className="min-w-[50px]" />
                      )}
                    </tr>
                  </thead>

                  <tbody>
                    {tasks.map((task) => {
                      const isTruck =
                        order.transports.find((transport) => transport.id == task.transportId)
                          ?.type === TransportType.TRUCK;

                      return (
                        <tr key={`task_${task.id}`}>
                          <td>{isTruck ? "Грузовик" : "Прицеп"}</td>
                          <td>{task.name}</td>
                          <td>{task.description || "-"}</td>
                          <td>
                            {Number(task.price || 0).toLocaleString()} руб
                          </td>
                          {user.role === UserRole.MASTER && (
                            <td className="flex">
                              <div
                                onClick={() => editTask(task)}
                                className="w-[15px] h-[15px] mr-2 relative cursor-pointer"
                              >
                                <Image src="/icons/pen.svg" fill alt="Edit" />
                              </div>

                              <div
                                onClick={() => deleteTask(task)}
                                className="w-[15px] h-[15px] relative cursor-pointer"
                              >
                                <Image
                                  src="/icons/trash.svg"
                                  fill
                                  alt="Delete"
                                />
                              </div>
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {user.role === UserRole.MASTER && (
            <Button
              onClick={() => setAddTask(true)}
              size="xs"
              variant="outline"
            >
              Добавить ремонтную работу
            </Button>
          )}
        </>
      )}

      {(editingTask || isAddTask) && (
        <TaskDialogComponent
          taskApiRepository={taskApiRepository}
          order={order}
          onTaskUpdated={() => {
            setAddTask(false);
            setEditingTask(undefined);
            loadTasks();
          }}
          closeDialog={() => {
            setAddTask(false);
            setEditingTask(undefined);
          }}
          task={editingTask}
        />
      )}
    </div>
  );
};
