"use client";
import {useEffect, useState} from "react";
import {Button, Table, TextInput} from "@mantine/core";
import UserApiRepository from "@/features/user/data/UserApiRepository";
import {UserRole} from "@/features/user/domain/UserRole";
import {User} from "@/features/user/domain/User";
import {PhoneInput} from "@/util/components/PhoneInput";
import {Mechanic} from "@/features/common/transport/domain/Mechanic";


interface Props {
    userApiRepository: UserApiRepository
}

export const MechanicComponent = ({userApiRepository}: Props): JSX.Element => {

    const [user, setUser] = useState<User | undefined>();
    const [phoneError, setPhoneError] = useState(false);
    const mechanics = new Map();

    // TODO upload mechanics
    mechanics.set(-1, useState( {
        id: -1,
        column: "",
        name: "",
        telephone: ""
    }));
    mechanics.set(1, useState({
        id: 1,
        column: "001",
        name: "Иванов Иван А",
        telephone: "79008882233",
    }));
    mechanics.set(2, useState({
        id: 2,
        column: "002",
        name: "Иванов Иван А",
        telephone: "79008882233",
    }));
    mechanics.set(3, useState({
        id: 3,
        column: "003",
        name: "Иванов Иван А",
        telephone: "79008882233",
    }));

    useEffect(() => {
        (async () => {
            try {
                const user = await userApiRepository.getUserById(
                    userApiRepository.getAuthorizedData().authorizedUserId
                );
                setUser(user);
            } catch (e) {
                alert("Ошибка при загрузке профиля. Попробуйте перезагрузить страницу");
                console.log((e as Error).message);
            }
        })();
    }, []);


    return (
        <div className="flex mt-6">
            <div>
                {user && user?.role !== UserRole.MASTER && (
                    <>
                        <div className="hidden sm:bloc">
                            <Button
                                onClick={() => {}}
                            >
                                Добавить
                            </Button>
                        </div>

                        <div className="mr-4">
                            <Button
                                onClick={() => {

                                }}
                                fullWidth
                            >
                                Добавить
                            </Button>
                        </div>
                    </>
                )}
            </div>
            <Table.ScrollContainer minWidth={500}>
                <Table >
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th >Колонна</Table.Th>
                            <Table.Th>Механик</Table.Th>
                            <Table.Th>Телефон</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {Array.from(mechanics.values()).map((mechanicState) => {
                            const [mechanic, setMechanic] = mechanicState;
                            return  (
                                <Table.Tr key={mechanic.id}>
                                    <Table.Td h={40} w={20}>
                                        <TextInput
                                            value={mechanic.column}
                                            onChange={(event) => {
                                                const newDriver =  {
                                                    id: mechanic.id,
                                                    name: mechanic.name,
                                                    telephone: mechanic.telephone,
                                                    column: event.currentTarget.value
                                                }
                                                setMechanic(newDriver);
                                                //TODO save driver
                                            }}
                                            placeholder="000"
                                        /></Table.Td>
                                    <Table.Td h={40}>
                                        <TextInput
                                            value={mechanic.name}
                                            onChange={(event) => {
                                                const newDriver =  {
                                                    id: mechanic.id,
                                                    name: event.currentTarget.value,
                                                    telephone: mechanic.telephone,
                                                    column: mechanic.column
                                                }
                                                setMechanic(newDriver);
                                                //TODO save driver
                                            }}
                                            placeholder="Иванов Иван И"
                                        />
                                    </Table.Td>
                                    <Table.Td h={40}>
                                        <PhoneInput
                                            phone={mechanic.telephone}
                                            error={phoneError}
                                            onChange={(value) => {
                                                const newDriver =  {
                                                    id: mechanic.id,
                                                    name: mechanic.name,
                                                    telephone: value,
                                                    column: mechanic.column
                                                }
                                                setMechanic(newDriver);
                                                //TODO save driver
                                            }}
                                        />
                                    </Table.Td>
                                </Table.Tr>
                            );
                        })}
                    </Table.Tbody>
                </Table>
            </Table.ScrollContainer>
        </div>
    );
}
