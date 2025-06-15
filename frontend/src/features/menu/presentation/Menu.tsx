"use client";
import React, { useEffect, useState } from "react";
import { MenuTab } from "../domain/MenuTab";
import Link from "next/link";
import { Navbar } from "../../navbar/Navbar";
import { Sidebar } from "./Sidebar";
import LoadingComponent from "../../../util/components/LoadingComponent";
import { AuthComponent } from "../../user/presentation/AuthComponent";
import { User } from "../../user/domain/User";
import ChangeListener from "../../../util/interfaces/ChangeListener";
import { UserRole } from "../../user/domain/UserRole";
import UserApiRepository from "../../user/data/UserApiRepository";
import { Button } from "@mantine/core";

interface Props {
  userApiRepository: UserApiRepository;
  children: React.ReactNode;
}

const tabs: MenuTab[] = [
  { name: "Заказы", url: "/", isAdminOnly: false },
  { name: "Профиль", url: "/profile", isAdminOnly: false },
  { name: "Пользователи", url: "/users", isAdminOnly: true },
  { name: "Статистика", url: "/statistics", isAdminOnly: true },
];

export const Menu = ({ userApiRepository, children }: Props): JSX.Element => {
  const [isLoadingAuth, setLoadingAuth] = useState(true);
  const [isAuthorized, setAuthorized] = useState(false);

  const [user, setUser] = useState<User | undefined>();

  const [selectedTab, setSelectedTab] = useState<MenuTab | undefined>();
  const [isSidebarOpen, setSidebarOpen] = useState(false);

  const loadUser = async () => {
    try {
      setUser(
        await userApiRepository.getUserById(
          userApiRepository.getAuthorizedData().authorizedUserId
        )
      );
    } catch (e) {
      console.log("Menu.loadUser: " + (e as Error).message);
      alert("Не вышло загрузить профиль. Пожалуйста, перезагрузите страницу");
    }
  };

  useEffect(() => {
    setSelectedTab(tabs.find((tab) => tab.url === window.location.pathname));
  }, []);

  useEffect(() => {
    (async () => {
      const isAuthorized = userApiRepository.isAuthorized();
      setAuthorized(isAuthorized);
      setLoadingAuth(false);

      if (isAuthorized) loadUser();

      const authListener: ChangeListener = {
        name: "Menu.addAuthListener",
        onChanged: () => {
          if (userApiRepository.isAuthorized()) {
            setAuthorized(true);
            loadUser();
          } else {
            window.location.reload();
          }
        },
      };
      userApiRepository.addAuthListener(authListener);
    })();
  }, []);

  // Auth by Telegram
  useEffect(() => {
    (async () => {
      const urlSearchParams = new URLSearchParams(window.location.search);
      const id = urlSearchParams.get("id");
      if (id) {
        try {
          await userApiRepository.connectTelegramViaTgAuth(
            decodeURIComponent(window.location.search.split("?")[1])
          );
          loadUser();
        } catch (e) {
          alert((e as Error).message);
        }

        window.history.replaceState(undefined, "", "/");
        window.history.pushState(undefined, "", "/");
      }
    })();
  }, []);

  // Sign in by admin token
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get("accessToken");
    const id = urlParams.get("userId");

    if (token && id) {
      userApiRepository.saveAuthorizedData(token, Number(id));
      userApiRepository.notifyAuthListeners();

      const href = window.location.href;
      const baseUrl = href.split("?")[0];
      const searchWithAuth = href.split("?")[1] || "";
      const searchWithoutAuth = searchWithAuth
        .split("&")
        .filter(
          (param) => !param.includes("accessToken") && !param.includes("userId")
        )
        .join("&");

      window.history.replaceState(undefined, "", baseUrl + searchWithoutAuth);
      window.history.pushState(undefined, "", baseUrl + searchWithoutAuth);
    }
  }, [userApiRepository]);

  return (
    <div className="mx-3 xl:mx-0">
      {isLoadingAuth ? (
        <div className="w-full flex">
          <div className="mx-auto mt-20">
            <LoadingComponent />
          </div>
        </div>
      ) : (
        <>
          {isAuthorized ? (
            <div>
              <Navbar
                isSidebar
                openSidebar={() => setSidebarOpen(true)}
                isMdPadding={true}
              />

              {isSidebarOpen && (
                <Sidebar
                  selectedTab={selectedTab}
                  tabs={tabs.filter(
                    (tab) => user?.role === UserRole.ADMIN || !tab.isAdminOnly
                  )}
                  closeSidebar={() => setSidebarOpen(false)}
                />
              )}

              <div className="xl:flex">
                <div className="w-full xl:w-[290px] sm:mr-10 xl:ml-10 mt-6">
                  <div
                    className="rounded shadow p-4 text-xs border-gray-200 mb-4"
                    style={{ borderWidth: "1px" }}
                  >
                    {user ? (
                      <>
                        <div className="max-w-full overflow-x-hidden whitespace-nowrap">
                          {user.role === UserRole.DRIVER ? "Имя" : "Название"}:{" "}
                          {user.name}
                        </div>
                        {user.inn && (
                          <div className="max-w-full overflow-x-hidden whitespace-nowrap">
                            ИНН: {user.inn}
                          </div>
                        )}
                        <div className="max-w-full overflow-x-hidden whitespace-nowrap">
                          Тип:&nbsp;
                          {user.role === UserRole.ADMIN && "администратор"}
                          {user.role === UserRole.DRIVER && "водитель"}
                          {user.role === UserRole.CUSTOMER && "перевозчик"}
                          {user.role === UserRole.MASTER && "автосервис"}
                        </div>

                        {user.role === UserRole.MASTER && (
                          <div className="mt-2">
                            Баланс: {user.balance.toLocaleString()} руб{" "}
                            <div
                              className="inline-block cursor-pointer text-gray-600 hover:underline"
                              onClick={() => {
                                window.location.href = "/increase-balance";
                              }}
                            >
                              (пополнить)
                            </div>
                          </div>
                        )}

                        {user.role === UserRole.MASTER && (
                          <>
                            {!user.connectedTelegramChats ||
                            user.connectedTelegramChats.length === 0 ? (
                              <Button
                                className="mt-3"
                                onClick={() =>
                                  (window.location.href = "/profile")
                                }
                                size="xs"
                                fullWidth
                                variant="outline"
                              >
                                Подключить уведомления в Telegram
                              </Button>
                            ) : (
                              <div className="max-w-full overflow-x-hidden whitespace-nowrap">
                                Telegram подключен{" "}
                                <div
                                  className="inline-block cursor-pointer text-gray-600 hover:underline"
                                  onClick={() =>
                                    (window.location.href = "/profile")
                                  }
                                >
                                  (сменить)
                                </div>
                              </div>
                            )}
                          </>
                        )}
                      </>
                    ) : (
                      <div className="w-full h-[50px] flex justify-center items-center">
                        <LoadingComponent />
                      </div>
                    )}
                  </div>

                  <div className="hidden xl:block">
                    {tabs
                      .filter(
                        (tab) =>
                          user?.role === UserRole.ADMIN || !tab.isAdminOnly
                      )
                      .map((tab) => {
                        const isSelected = tab.url == selectedTab?.url;

                        return (
                          <Link
                            key={`tab_${tab.url}`}
                            className={`${
                              isSelected ? "!bg-blue-700 text-white" : ""
                            } hover:bg-blue-100 cursor-pointer py-2 px-4 mb-2 rounded-lg text-sm block`}
                            href={tab.url}
                          >
                            {tab.name}
                          </Link>
                        );
                      })}
                  </div>
                </div>

                <div className="w-full">{children}</div>
              </div>
            </div>
          ) : (
            <AuthComponent userApiRepository={userApiRepository} />
          )}
        </>
      )}
    </div>
  );
};
