"use client";
import { Menu } from "../../features/menu/presentation/Menu";
import moment from "moment";
import "moment/locale/ru";
import {
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY,
  APPLICATION_SERVER,
  Pages,
} from "../../constants";
import UserApiRepository from "../../features/user/data/UserApiRepository";
import ApiHelper from "../../util/api/ApiHelper";
import { useEffect, useState } from "react";
import { User } from "../../features/user/domain/User";
import LoadingComponent from "../../util/components/LoadingComponent";
import { UserRole } from "../../features/user/domain/UserRole";
import styles from "./page.module.css";
import { ClipboardHelper } from "../../util/ClipboardHelper";
import StringUtils from "../../util/StringUtils";
import { MultiSelect } from "@mantine/core";
import { useRegions } from "../../features/regions/hooks/useRegions";

const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);

const USERS_FILTERING_REGIONS_KEY = "users_filtering_regions_key";
const USERS_FITLERING_ROLES_KEY = "users_filtering_statuses_key";

export default function Page() {
  const [users, setUsers] = useState<User[]>([]);
  const regions = useRegions();

  // filters
  const [filteringRegionsIds, setFilteringRegions] = useState<
    string[] | undefined
  >();
  const [filteringUserRoles, setFilteringUserRoles] = useState<
    string[] | undefined
  >();

  const loadUsers = async () => {
    if (!filteringRegionsIds || !filteringUserRoles) return;

    try {
      setUsers(
        await userApiRepository.getUsers(
          filteringRegionsIds.map((r) => +r),
          filteringUserRoles
        )
      );
    } catch (e) {
      alert((e as Error).message);
    }
  };

  const openUserOrders = (userId: number) => {
    window
      .open(`${Pages.HOME_PAGE}?filteringUserId=${userId}`, "_blank")
      ?.focus();
  };

  const getAccessToken = async (userId: number) => {
    try {
      const userAuth = await userApiRepository.getUserAccess(userId);
      const authUrl = `${APPLICATION_SERVER}?userId=${userAuth.id}&accessToken=${userAuth.accessToken}`;
      ClipboardHelper.copyDataToClipboard(authUrl);
    } catch (e) {
      alert((e as Error).message);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [filteringRegionsIds, filteringUserRoles]);

  // restore filters
  useEffect(() => {
    const filteringRegionsIds = localStorage.getItem(
      USERS_FILTERING_REGIONS_KEY
    );
    const filteringUserRoles = localStorage.getItem(USERS_FITLERING_ROLES_KEY);

    setFilteringRegions(
      filteringRegionsIds ? JSON.parse(filteringRegionsIds) : []
    );
    setFilteringUserRoles(
      filteringUserRoles ? JSON.parse(filteringUserRoles.toUpperCase()) : []
    );
  }, []);

  console.log(
    `regions:\n${JSON.stringify(regions.slice(0, 10), undefined, 4)}`
  );

  return (
    <Menu userApiRepository={userApiRepository}>
      <main className="mb-20 mt-6">
        <div className="text-lg font-bold mb-5">Пользователи</div>

        <div className="mb-3 flex">
          <div className="max-w-[400px] mr-3">
            <MultiSelect
              label="Тип пользователей"
              placeholder="Выберите типы пользователей"
              clearable
              data={[
                {
                  label: "перевозчик",
                  value: UserRole.CUSTOMER,
                },
                {
                  label: "автосервис",
                  value: UserRole.MASTER,
                },
                {
                  label: "водитель",
                  value: UserRole.DRIVER,
                },
                {
                  label: "администратор",
                  value: UserRole.ADMIN,
                },
              ]}
              value={filteringUserRoles}
              onChange={(value) => {
                setFilteringUserRoles(value);
                localStorage.setItem(
                  USERS_FITLERING_ROLES_KEY,
                  JSON.stringify(value)
                );
              }}
              searchable
            />
          </div>

          {regions.length > 0 ? (
            <div className="max-w-[400px] mr-3">
              <MultiSelect
                clearable
                label="Регионы пользователей"
                placeholder="Выберите регионы"
                data={regions.map((region) => {
                  return {
                    label: region.name,
                    value: `${region.id}`,
                  };
                })}
                value={filteringRegionsIds}
                onChange={(value) => {
                  setFilteringRegions(value);
                  localStorage.setItem(
                    USERS_FILTERING_REGIONS_KEY,
                    JSON.stringify(value)
                  );
                }}
                searchable
              />
            </div>
          ) : (
            <LoadingComponent size="xs" />
          )}
        </div>

        <div>
          {users.length === 0 ? (
            <LoadingComponent />
          ) : (
            <div className="mb-5 mr-3">
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th className="min-w-[60px]">#</th>
                    <th className="min-w-[200px]">Название</th>
                    <th className="min-w-[90px] max-w-[90px]">Тип</th>
                    <th className="min-w-[130px]">Регион</th>
                    <th className="min-w-[200px]">Почта</th>
                    <th className="min-w-[160px] max-w-[160px]">Номер</th>
                    <th className="min-w-[90px] max-w-[90px]">Баланс</th>
                    <th className="min-w-[210px]">Дата регистрации</th>
                    <th className="min-w-[80px]">Действия</th>
                  </tr>
                </thead>

                <tbody>
                  {users.map((user) => {
                    return (
                      <tr key={`user_${user.id}`}>
                        <td
                          className="text-blue-600 cursor-pointer"
                          onClick={() => getAccessToken(user.id)}
                        >
                          {user.id}
                        </td>
                        <td>{user.name}</td>
                        <td className="text-center">
                          {user.role === UserRole.ADMIN && "администратор"}
                          {user.role === UserRole.CUSTOMER && "перевозчик"}
                          {user.role === UserRole.DRIVER && "водитель"}
                          {user.role === UserRole.MASTER && "автосервис"}
                        </td>
                        <td>{user.address?.regionName}</td>
                        <td>{user.email}</td>
                        <td className="text-center">
                          {StringUtils.formatAsPhone(user.phone)}
                        </td>
                        <td className="text-center">
                          {user.role === UserRole.MASTER
                            ? user.balance.toLocaleString()
                            : ""}
                        </td>
                        <td style={{ fontSize: "10px" }}>
                          {moment(user.registrationDate).format("lll")} (
                          {moment(user.registrationDate).fromNow()})
                        </td>

                        <td
                          className="text-center text-blue-600 cursor-pointer"
                          onClick={() => openUserOrders(user.id)}
                        >
                          Заказы
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </Menu>
  );
}
