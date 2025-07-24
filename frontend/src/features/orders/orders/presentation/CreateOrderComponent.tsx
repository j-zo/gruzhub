"use client";
import { useEffect, useState } from "react";
import UserApiRepository from "../../../user/data/UserApiRepository";
import {
  Autocomplete,
  Button,
  Checkbox,
  Select,
  TextInput,
  Textarea,
} from "@mantine/core";
import {
  TRUCK_BRANDS,
  TRUCK_MODELS,
  TRUCK_PROBLEMS,
} from "../../../../util/truck_constants";
import {
  TRAILER_PROBLEMS,
  TRAILER_TYPE,
} from "../../../../util/trailer_constants";
import { PhoneInput } from "../../../../util/components/PhoneInput";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import Link from "next/link";
import { OrderApiRepository } from "../data/OrderApiRepository";
import { CreateOrderRequest } from "../domain/dto/CreateOrderRequest";
import { TransportType } from "@/features/common/transport/domain/TransportType";
import { Transport } from "@/features/common/transport/domain/Transport";
import { Random } from "../../../../util/Random";
import { User } from "../../../user/domain/User";
import { UserRole } from "../../../user/domain/UserRole";
import StringUtils from "../../../../util/StringUtils";
import { useRegions } from "../../../regions/hooks/useRegions";

interface Props {
  userApiRepository: UserApiRepository;
  orderApiRepository: OrderApiRepository;

  onOrderCreated?(): void;
}

export const CreateOrderComponent = ({
  userApiRepository,
  orderApiRepository,

  onOrderCreated,
}: Props): JSX.Element => {
  const [isPosting, setPosting] = useState(false);

  const [isUserLoading, setUserLoading] = useState(false);
  const [user, setUser] = useState<User | undefined>();

  const regions = useRegions();

  const [isTruck, setTruck] = useState(false);
  const [truckBrand, setTruckBrand] = useState("");
  const [truckModel, setTruckModel] = useState("");
  const [truckProblems, setTruckProblems] = useState<string[]>([]);

  const [isTrailer, setTrailer] = useState(false);
  const [trailerModel, setTrailerModel] = useState("");
  const [trailerProblems, setTrailerProblems] = useState<string[]>([]);

  const [regionId, setRegionId] = useState<number | undefined>();

  const [isNeedEvacuator, setNeedEvacuator] = useState(false);
  const [isNeedMobileTeam, setNeedMobileTeam] = useState(false);

  const [notes, setNotes] = useState("");
  const [urgency, setUrgency] = useState("");

  const [driverName, setDriverName] = useState("");
  const [driverPhone, setDriverPhone] = useState("");

  const [truckBrandError, setTruckBrandError] = useState(false);

  const [driverNameError, setDriverNameError] = useState(false);
  const [driverPhoneError, setDriverPhoneError] = useState(false);

  const [regionError, setRegionError] = useState(false);
  const [urgencyError, setUrgencyError] = useState(false);

  const [postError, setPostError] = useState("");

  const validateFieldsForOrderCreation = (): boolean => {
    if (!isTrailer && !isTruck) {
      setPostError(
        "Выберите, что нужно отремантировать: грузовик и (или) прицеп"
      );
      return false;
    }

    if (isTruck) {
      if (!truckBrand) {
        setTruckBrandError(true);
        return false;
      }
    }

    if (user?.role !== UserRole.DRIVER) {
      if (!driverName) {
        setDriverNameError(true);
        return false;
      }

      if (!driverPhone || driverPhone.length < 11) {
        setDriverPhoneError(true);
        return false;
      }
    }

    if (!regionId) {
      setRegionError(true);
      return false;
    }

    if (!urgency) {
      setUrgencyError(true);
      return false;
    }

    return true;
  };

  const postOrder = async () => {
    setPostError("");
    if (!validateFieldsForOrderCreation()) return;

    try {
      setPosting(true);

      let truckProblemsString = isTruck ? "Требуется ремонт грузовика" : "";
      let trailerProblemsString = "";

      truckProblems.forEach(
        (problem) => (truckProblemsString += `\n- ${problem}`)
      );

      if (isTrailer) {
        if (isTruck) trailerProblemsString += "\n\n";
        trailerProblemsString += "Требуется ремонт прицепа";
      }
      trailerProblems.forEach(
        (problem) => (trailerProblemsString += `\n- ${problem}`)
      );

      const transports: Transport[] = [];
      if (isTruck) {
        transports.push({
          id: undefined as unknown as number,
          brand: truckBrand,
          model: truckModel,
          type: TransportType.TRUCK,
          parkNumber: "1"
          // TODO parknumber
        });
      }
      if (isTrailer) {
        transports.push({
          id: undefined as unknown as number,
          model: trailerModel,
          type: TransportType.TRAILER,
          parkNumber: "1"
          //TODO
        });
      }

      const orderToCreate: CreateOrderRequest = {
        guaranteeUuid: `${Random.generateUuid(20)}-${Date.now()}`,

        driverName: driverName || user?.name || "",
        driverPhone: driverPhone
          ? StringUtils.cleanPhoneFormatting(driverPhone)
          : undefined || user?.phone || "",

        regionId: regionId as unknown as number,
        description: truckProblemsString + trailerProblemsString,
        notes,
        
        isNeedEvacuator,
        isNeedMobileTeam,

        transports: transports,

        urgency: urgency.toLowerCase(),
      };

      const response = await orderApiRepository.postOrder(orderToCreate);
      if (response.accessToken && response.driverId) {
        userApiRepository.saveAuthorizedData(
          response.accessToken,
          response.driverId
        );
      }

      if (onOrderCreated) onOrderCreated();

      if (window.location.href !== "/") {
        window.location.href = "/";
      }
    } catch (e) {
      setPostError((e as Error).message);
    }

    setPosting(false);
  };

  useEffect(() => {
    (async () => {
      setUserLoading(true);

      try {
        if (userApiRepository.isAuthorized()) {
          const user = await userApiRepository.getUserById(
            userApiRepository.getAuthorizedData().authorizedUserId
          );
          setUser(user);

          if (user.role === UserRole.DRIVER) {
            setDriverName(user.name);
            setDriverPhone(user.phone);
          }
        }
      } catch (e) {
        console.log((e as Error).message);
        alert(
          "Ошибка загрузки пользователя. Пожалуйста, перезагрузите страницу"
        );
      }

      setUserLoading(false);
    })();
  }, []);

  return (
    <div>
      <Checkbox
        checked={isTruck}
        onChange={() => {
          setTruck(!isTruck);
          if (isTruck) setTruckProblems([]);
        }}
        label="Нужен ремонт грузовика"
      />

      {isTruck && (
        <>
          <Autocomplete
            className="mt-2"
            required
            label="Марка машины"
            placeholder="Mercedes"
            data={TRUCK_BRANDS}
            value={truckBrand}
            error={truckBrandError}
            onChange={(value) => {
              setTruckBrandError(false);
              setTruckBrand(value || "");
            }}
          />

          <Autocomplete
            className="mt-1"
            label="Модель машины"
            description="Если не знаете - оставьте пустым"
            placeholder="Actros"
            data={TRUCK_MODELS[truckBrand]}
            value={truckModel}
            onChange={(value) => {
              setTruckModel(value || "");
            }}
          />

          <div className="mt-5" />

          <div className="mt-4 text-[14px] font-medium">
            Выберите предполагаемую проблему
          </div>

          {TRUCK_PROBLEMS.map((problem) => {
            return (
              <div key={`truck_problem_${problem}`}>
                <Checkbox
                  className="mt-2"
                  checked={truckProblems.includes(problem)}
                  onChange={() => {
                    if (truckProblems.includes(problem)) {
                      truckProblems.splice(truckProblems.indexOf(problem), 1);
                    } else {
                      truckProblems.push(problem);
                    }
                    setTruckProblems(JSON.parse(JSON.stringify(truckProblems)));
                  }}
                  label={problem}
                  size="xs"
                />
              </div>
            );
          })}
        </>
      )}

      <Checkbox
        className={isTruck ? "mt-7" : "mt-2"}
        checked={isTrailer}
        onChange={() => {
          setTrailer(!isTrailer);
          if (isTrailer) setTrailerProblems([]);
        }}
        label="Нужен ремонт прицепа"
      />

      {isTrailer && (
        <>
          <Autocomplete
            className="mt-2"
            label="Тип прицепа"
            placeholder="Тентованный"
            data={TRAILER_TYPE}
            value={trailerModel}
            onChange={(value) => {
              setTrailerModel(value || "");
            }}
          />

          <div className="mt-4 text-[14px] font-medium">
            Выберите предполагаемую проблему
          </div>

          {TRAILER_PROBLEMS.map((problem) => {
            return (
              <div key={`truck_problem_${problem}`}>
                <Checkbox
                  className="mt-2"
                  checked={trailerProblems.includes(problem)}
                  onChange={() => {
                    if (trailerProblems.includes(problem)) {
                      trailerProblems.splice(
                        trailerProblems.indexOf(problem),
                        1
                      );
                    } else {
                      trailerProblems.push(problem);
                    }
                    setTrailerProblems(
                      JSON.parse(JSON.stringify(trailerProblems))
                    );
                  }}
                  label={problem}
                  size="xs"
                />
              </div>
            );
          })}
        </>
      )}

      <div className="mt-5" />

      {regions.length === 0 ? (
        <div className="flex justify-center">
          <LoadingComponent size="xs" />
        </div>
      ) : (
        <>
          <Select
            label="Регион, где требуется ремонт"
            placeholder="Ленинградская область"
            data={regions.map((region) => region.name)}
            searchable
            value={regions.find((region) => region.id === regionId)?.name}
            error={regionError}
            onChange={(value) => {
              setRegionError(false);
              setRegionId(regions.find((region) => region.name === value)?.id);
            }}
          />
        </>
      )}

      <Checkbox
        className="mt-2"
        checked={isNeedMobileTeam}
        onChange={() => setNeedMobileTeam(!isNeedMobileTeam)}
        label="Нужна выездная бригада"
      />

      <Checkbox
        className="mt-2"
        checked={isNeedEvacuator}
        onChange={() => setNeedEvacuator(!isNeedEvacuator)}
        label="Нужен эвакуатор"
      />

      <Select
        className="mt-3"
        label="Как срочно требуется ремонт?"
        data={[
          "Срочно (сегодня)",
          "Средне-срочно (завтра или в ближайшие дни)",
          "Нужен капитальный ремонт",
        ]}
        placeholder="Выберите срочность"
        searchable
        value={urgency}
        error={urgencyError}
        onChange={(value) => {
          setUrgencyError(false);
          setUrgency(value || "");
        }}
      />

      <Textarea
        className="mt-1"
        label="Примечания"
        placeholder="Напишите, если хотите что-то дополнить"
        value={notes}
        onChange={(e) => {
          setNotes(e.currentTarget.value);
        }}
      />

      <TextInput
        required
        className="mt-3"
        label="Имя водителя"
        placeholder="Иванов Иван"
        value={driverName}
        error={driverNameError}
        onChange={(e) => {
          setDriverNameError(false);
          setDriverName(e.currentTarget.value);
        }}
      />

      <PhoneInput
        required
        label="Номер телефона водителя"
        phone={driverPhone}
        error={driverPhoneError}
        onChange={(value) => {
          setDriverPhone(value);
          setDriverPhoneError(false);
        }}
      />

      <Button
        disabled={isPosting || isUserLoading}
        className="mt-5"
        fullWidth
        onClick={() => postOrder()}
      >
        Найти автосервис
      </Button>

      {postError && (
        <div className="mt-3 flex justify-center text-red-500 text-sm text-center mx-5">
          {postError}
        </div>
      )}

      <div className="mt-5 text-sm text-center text-gray-600">
        После отправки заявки с вами свяжется наиболее подходящий автосервис
        (его можно будет сменить)
      </div>

      <div className="mt-5 text-sm text-gray-600 text-center">
        <div>По всем вопросам звоните или пишите:</div>
        <div>
          <Link href="tel:+79950084323">+7 995 008 4323</Link> (WhatsApp)
        </div>
      </div>
    </div>
  );
};
