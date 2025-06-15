"use client";
import { Menu } from "../../features/menu/presentation/Menu";
import {
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY,
} from "../../constants";
import UserApiRepository from "../../features/user/data/UserApiRepository";
import ApiHelper from "../../util/api/ApiHelper";
import {
  Button,
  Checkbox,
  Modal,
  NumberInput,
  Select,
  TextInput,
} from "@mantine/core";
import { useEffect, useState } from "react";
import { User } from "../../features/user/domain/User";
import LoadingComponent from "../../util/components/LoadingComponent";
import { UserRole } from "../../features/user/domain/UserRole";
import { PhoneInput } from "../../util/components/PhoneInput";
import FormValidator from "../../util/FormValidator";
import StringUtils from "../../util/StringUtils";
import { useRegions } from "../../features/regions/hooks/useRegions";
import { useCountries } from "../../features/regions/hooks/useCountries";

const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);

export default function Page() {
  const regions = useRegions(/* isOnlyRussia */ false);
  const countries = useCountries();

  const [user, setUser] = useState<User | undefined>();
  const [isSaving, setSaving] = useState(false);
  const [isUnsaved, setUnsaved] = useState(false);

  const [countryCode, setCountryCode] = useState("RU");

  const [isMobileTeam, setMobileTeam] = useState(false);
  const [isChangePassword, setChangePassword] = useState(false);

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [nameError, setNameError] = useState(false);
  const [innError, setInnError] = useState(false);

  const [regionError, setRegionError] = useState(false);
  const [cityError, setCityError] = useState(false);
  const [streetError, setStreetError] = useState(false);

  const [emailError, setEmailError] = useState(false);
  const [phoneError, setPhoneError] = useState(false);

  const [passwordError, setPasswordError] = useState(false);
  const [confirmPasswordError, setConfirmPasswordError] = useState(false);

  const [updateError, setUpdateError] = useState("");

  const [isShowAddEmployee, setShowAddEmployee] = useState(false);

  const [creatingChatCode, setCreatingChatCode] = useState<string>("");

  const updateUser = (user: User) => {
    setUser(JSON.parse(JSON.stringify(user)));
    setUnsaved(true);
  };

  const validateFieldsForUpdate = (): boolean => {
    if (!user?.name) {
      setNameError(true);
      return false;
    }

    if (user.role === UserRole.CUSTOMER || user.role === UserRole.MASTER) {
      if (!user.inn || `${user.inn}`.length > 12 || `${user.inn}`.length < 10) {
        setInnError(true);
        return false;
      }

      if (!user.address?.region?.id) {
        setRegionError(true);
        return false;
      }

      if (user.role === UserRole.MASTER) {
        if (!user.address?.city) {
          setCityError(true);
          return false;
        }

        if (!user.address?.street) {
          setStreetError(true);
          return false;
        }
      }

      if (!user.email) {
        setEmailError(true);
        return false;
      } else if (!FormValidator.isValidEmail(user.email)) {
        setEmailError(true);
        return false;
      }
    }

    if (!StringUtils.cleanPhoneFormatting(user.phone)) {
      setPhoneError(true);
      return false;
    } else if (user.phone.length < 11) {
      setPhoneError(true);
      return false;
    }

    if (password) {
      if (!confirmPassword) {
        setConfirmPasswordError(true);
        return false;
      } else if (password !== confirmPassword) {
        setConfirmPasswordError(true);
        return false;
      }
    }

    return true;
  };

  const saveUser = async () => {
    if (validateFieldsForUpdate() && user) {
      setSaving(true);

      try {
        await userApiRepository.updateUser({
          id: user.id,
          role: user.role,

          name: user.name,
          inn: user.inn,

          email: user.email,
          phone: StringUtils.cleanPhoneFormatting(user.phone),
          password: password ? password : undefined,

          tripRadiusKm: user.tripRadiusKm,

          regionId: user.address?.region?.id,
          city: user.address?.city,
          street: user.address?.street,
        });
        setUnsaved(false);
        setChangePassword(false);
        setPassword("");
        setConfirmPassword("");
      } catch (e) {
        setUpdateError((e as Error).message);
      }

      setSaving(false);
    }
  };

  const connectTelegramChat = async () => {
    if (user) {
      try {
        await userApiRepository.connectTelegramChat(creatingChatCode);
        window.location.reload();
      } catch (e) {
        alert((e as Error).message);
      }
    }
  };

  const disconnectTelegramChat = async (chatUuid: string) => {
    if (user) {
      try {
        await userApiRepository.disconnectTelegramChat(chatUuid);
        window.location.reload();
      } catch (e) {
        alert((e as Error).message);
      }
    }
  };

  useEffect(() => {
    (async () => {
      try {
        if (!userApiRepository.isAuthorized()) {
          window.location.href = "/";
          return;
        }

        const user = await userApiRepository.getUserById(
          userApiRepository.getAuthorizedData().authorizedUserId
        );
        setUser(user);

        if (user.tripRadiusKm && user.tripRadiusKm > 0) setMobileTeam(true);
      } catch (e) {
        console.log((e as Error).message);
        alert("Не вышло загрузить профиль. Пожалуйста, перезагрузите страницу");
      }
    })();
  }, []);

  useEffect(() => {
    if (regions.length > 0 && user) {
      const userRegionId = user.address?.region?.id;
      if (userRegionId) {
        const userRegion = regions.find((region) => region.id === userRegionId);
        if (userRegion) {
          setCountryCode(userRegion.country.code);
        }
      }
    }
  }, [regions, user]);

  return (
    <Menu userApiRepository={userApiRepository}>
      <main className="max-w-xs mb-20">
        <div className="text-lg font-bold mb-10 mt-6">Профиль</div>

        {!user ? (
          <>
            <LoadingComponent />
          </>
        ) : (
          <>
            <TextInput
              label={
                user.role === UserRole.DRIVER ? "Ваше имя" : "Название компании"
              }
              placeholder={
                user.role === UserRole.DRIVER
                  ? "Иванов Иван"
                  : 'ООО "Грузовые машины"'
              }
              value={user.name}
              error={nameError}
              onChange={(e) => {
                setNameError(false);
                user.name = e.currentTarget.value;
                updateUser(user);
              }}
            />

            {(user.role === UserRole.CUSTOMER ||
              user.role === UserRole.MASTER) && (
              <>
                <TextInput
                  label="ИНН"
                  placeholder="772842103808"
                  value={user.inn}
                  error={innError}
                  onChange={(e) => {
                    setInnError(false);
                    user.inn = e.currentTarget.value
                      .replace(/[^0-9]/g, "")
                      .slice(0, 12);
                    updateUser(user);
                  }}
                />

                {regions.length === 0 ? (
                  <div className="flex justify-center mt-4 mb-1">
                    <LoadingComponent size="xs" />
                  </div>
                ) : (
                  <>
                    {user.role === UserRole.CUSTOMER && (
                      <Select
                        label="Страна"
                        placeholder="Выберите страну"
                        data={countries.map((country) => {
                          return {
                            label: country.name,
                            value: country.code,
                          };
                        })}
                        searchable
                        value={countryCode}
                        onChange={(value) => {
                          setCountryCode(value || "RU");

                          if (user.address) {
                            user.address.region = regions.find(
                              (region) => region.country.code === value
                            );
                          }
                          updateUser(user);
                        }}
                      />
                    )}

                    <Select
                      label="Регион"
                      placeholder="Ленинградская область"
                      data={regions
                        .filter((region) => region.country.code === countryCode)
                        .map((region) => region.name)}
                      searchable
                      value={
                        regions.find(
                          (region) => region.id === user.address?.region?.id
                        )?.name
                      }
                      error={regionError}
                      onChange={(value) => {
                        setRegionError(false);

                        if (user.address) {
                          user.address.region = regions.find(
                            (region) => region.name === value
                          );
                        }

                        updateUser(user);
                      }}
                    />
                  </>
                )}

                {user.role === UserRole.MASTER && (
                  <>
                    <TextInput
                      label="Город"
                      placeholder="Санкт-Петербург"
                      value={user.address?.city}
                      error={cityError}
                      onChange={(e) => {
                        setCityError(false);
                        if (user.address) {
                          user.address.city = e.currentTarget.value;
                        }
                        updateUser(user);
                      }}
                    />

                    <TextInput
                      label="Улица"
                      placeholder="Невский проспект 15"
                      value={user.address?.street}
                      error={streetError}
                      onChange={(e) => {
                        setStreetError(false);
                        if (user.address) {
                          user.address.street = e.currentTarget.value;
                        }
                        updateUser(user);
                      }}
                    />
                  </>
                )}
              </>
            )}

            {user.role === UserRole.MASTER && (
              <>
                <div className="mt-4">
                  <Checkbox
                    checked={isMobileTeam}
                    onChange={() => {
                      const isMobileTeamNewValue = !isMobileTeam;

                      setMobileTeam(isMobileTeamNewValue);
                      if (!isMobileTeamNewValue) {
                        user.tripRadiusKm = 0;
                      }
                    }}
                    label="Есть выездная бригада"
                  />
                </div>

                <div className="mt-1" />

                {isMobileTeam && (
                  <NumberInput
                    label="Радиус выезда в км"
                    value={user.tripRadiusKm}
                    onChange={(value) => {
                      user.tripRadiusKm = +value;
                      updateUser(user);
                    }}
                    min={0}
                    step={1}
                  />
                )}
              </>
            )}

            <div className="mt-7" />

            <TextInput
              label="Почта"
              placeholder="your@email.com"
              value={user.email}
              onChange={(e) => {
                setEmailError(false);
                user.email = e.currentTarget.value;
                updateUser(user);
              }}
              error={emailError}
              type="email"
            />

            <PhoneInput
              label="Номер телефона"
              phone={user.phone}
              error={phoneError}
              onChange={(value) => {
                setPhoneError(false);
                user.phone = value;
                updateUser(user);
              }}
            />

            <div className="mt-4" />

            <Checkbox
              checked={isChangePassword}
              onChange={() => {
                const isChangePasswordNewValue = !isChangePassword;

                setChangePassword(isChangePasswordNewValue);
                if (!isChangePasswordNewValue) {
                  setPassword("");
                  setPasswordError(false);
                  setConfirmPassword("");
                  setConfirmPasswordError(false);
                }
              }}
              label="Сменить пароль"
            />

            {isChangePassword && (
              <>
                <div className="mt-1" />

                <TextInput
                  label="Пароль"
                  placeholder="Введите пароль"
                  value={password}
                  error={passwordError}
                  onChange={(e) => {
                    setPasswordError(false);
                    setPassword(e.currentTarget.value);
                    setUnsaved(true);
                  }}
                  type="password"
                />

                <TextInput
                  label="Подтвердите пароль"
                  placeholder="Повторите пароль"
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.currentTarget.value);
                    setConfirmPasswordError(false);
                    setUnsaved(true);
                  }}
                  error={confirmPasswordError}
                  type="password"
                />
              </>
            )}

            {user.role === UserRole.MASTER && (
              <>
                <div className="mt-7" />

                <div className=" font-semibold text-sm mb-2">
                  Дополнительные Telegram чаты для уведомлений
                </div>

                <div className="mt-1" />

                {user.connectedTelegramChats?.map((chat, index) => {
                  return (
                    <div
                      key={`chat_${index}`}
                      className="flex mb-1 items-center"
                    >
                      <div className="mr-3 grow text-sm truncate">
                        {chat.title || "-"} ({chat.chatUuid})
                      </div>

                      <Button
                        className="ml-1"
                        type="button"
                        variant="outline"
                        color="red"
                        size="xs"
                        onClick={() => {
                          user.connectedTelegramChats.splice(
                            user.connectedTelegramChats.indexOf(chat),
                            1
                          );
                          disconnectTelegramChat(chat.chatUuid);
                        }}
                      >
                        x
                      </Button>
                    </div>
                  );
                })}

                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  fullWidth
                  onClick={() => {
                    setShowAddEmployee(true);
                  }}
                >
                  Добавить Telegram уведомления сотруднику
                </Button>
              </>
            )}
          </>
        )}

        <div className="mt-7" />

        {updateError && (
          <div className="mb-7 flex text-red-500 text-sm">{updateError}</div>
        )}

        <div className="sm:flex">
          {isUnsaved && (
            <>
              <div className="sm:hidden">
                <Button
                  disabled={isSaving}
                  fullWidth
                  onClick={() => saveUser()}
                >
                  Сохранить
                </Button>
              </div>

              <div className="hidden sm:block mr-2">
                <Button disabled={isSaving} onClick={() => saveUser()}>
                  Сохранить
                </Button>
              </div>
            </>
          )}

          <div className="sm:hidden mt-1">
            <Button
              color="red"
              fullWidth
              onClick={() => {
                userApiRepository.logout();
                window.location.href = "/";
              }}
            >
              Выход
            </Button>
          </div>

          <div className="hidden sm:block">
            <Button
              color="red"
              onClick={() => {
                userApiRepository.logout();
                window.location.href = "/";
              }}
            >
              Выход
            </Button>
          </div>
        </div>
      </main>

      {isShowAddEmployee && (
        <Modal
          opened
          onClose={() => {
            setShowAddEmployee(false);
            setCreatingChatCode("");
          }}
        >
          <div className="text-sm my-2">
            Чтобы получить код для добавления в ваш профиль, сотрудник со своего
            телефона должен написать /code нашему Telegram боту{" "}
            <a className="text-blue-600" href="https://t.me/gruzhub_bot">
              @gruzhub_bot
            </a>
          </div>

          <div className="mt-2">
            <TextInput
              label="Введите код сотрудника"
              placeholder="xxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              value={creatingChatCode.trim()}
              onChange={(e) => {
                setCreatingChatCode(e.currentTarget.value);
              }}
            />
          </div>

          <div className="mt-2">
            <Button
              onClick={() => {
                connectTelegramChat();
              }}
              fullWidth
            >
              Добавить
            </Button>
          </div>
        </Modal>
      )}
    </Menu>
  );
}
