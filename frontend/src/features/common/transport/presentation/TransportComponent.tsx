"use client";
import {useEffect, useState} from "react";
import {Button, ScrollArea, Table, TextInput} from "@mantine/core";
import UserApiRepository from "@/features/user/data/UserApiRepository";
import {UserRole} from "@/features/user/domain/UserRole";
import {User} from "@/features/user/domain/User";
import {TransportApiRepository} from "@/features/common/transport/data/TransportApiRepository";
import {PhoneInput} from "@/util/components/PhoneInput";

interface Props {
    userApiRepository: UserApiRepository,
    transportApiRepository: TransportApiRepository
}

export const TransportComponent = ({userApiRepository}: Props): JSX.Element => {

    const [user, setUser] = useState<User | undefined>();
    const [phoneError, setPhoneError] = useState(false);
    // TODO upload mechanics

    const mechanics = new Map();
    const drivers = new Map();
    drivers.set(-1, useState( {
        id: -1,
        name: "",
        phone: ""
    }))

    drivers.set(1, useState( {
        id: 1,
        name: "Vasiya",
        phone: "78273645271"
    }))

    const transports = new Map();
    transports.set(-1, useState( {
        id: -1,
        parkNumber: "",
        column: "",
        brand: "",
        model: "",
        year: -1,
        number: "",
        vin: ""
    }));

    transports.set(1, useState({
        id: 1,
        parkNumber: "1111",
        column: "001",
        brand: "Brand",
        model: "Model",
        year: 2019,
        number: "12345",
        vin: "12345"
    }));
    transports.set(2, useState({
        id: 2,
        parkNumber: "1111",
        column: "001",
        brand: "Brand",
        model: "Model",
        year: 2019,
        number: "12345",
        vin: "12345"
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
        <div className="flex flex-col">

            <div className="flex flex-row mt-6">
                <div className="w-[800px] pg-4">
                    <div className="flex flex-col space-y-4">
                        <div className="flex flex-row p2">
                            <div>
                                {user && user?.role !== UserRole.MASTER && (
                                    <>
                                        <div className="mr-4">
                                            <Button
                                                onClick={() => {

                                                }}
                                            >
                                                Добавить А/М
                                            </Button>
                                        </div>
                                    </>
                                )}
                            </div>
                            <div>
                                {user && user?.role !== UserRole.MASTER && (
                                    <>
                                        <div className="mr-4">
                                            <Button
                                                onClick={() => {

                                                }}
                                            >
                                                Создать заявку
                                            </Button>
                                        </div>
                                    </>
                                )}
                            </div>
                            <div>
                                {user && user?.role !== UserRole.MASTER && (
                                    <>
                                        <div className="mr-4">
                                            <Button
                                                onClick={() => {

                                                }}
                                            >
                                                Загрузить ТС из файла
                                            </Button>
                                        </div>
                                    </>
                                )}
                            </div>
                            <div>
                                {user && user?.role !== UserRole.MASTER && (
                                    <>
                                        <div className="mr-4">
                                            <Button
                                                onClick={() => {

                                                }}
                                            >
                                                Выгрузить ТС в файл
                                            </Button>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                        <div className="p2">
                            <Table className="min-w-[800px]">
                                <Table.Thead>
                                    <Table.Tr>
                                        <Table.Th>№ в парке</Table.Th>
                                        <Table.Th>№ колонны</Table.Th>
                                        <Table.Th>Марка</Table.Th>
                                        <Table.Th>Модель</Table.Th>
                                        <Table.Th>Гос. №</Table.Th>
                                        <Table.Th>VIN</Table.Th>
                                        <Table.Th></Table.Th>
                                    </Table.Tr>
                                </Table.Thead>
                                <Table.Tbody>
                                    {Array.from(transports.values()).map((transportState) => {
                                        const [transport, setTransport] = transportState;
                                        return  (
                                            <Table.Tr key={transport.id}>
                                                <Table.Td h={40} w={100}>
                                                    <TextInput
                                                        value={transport.parkNumber}
                                                        onChange={(event) => {
                                                            const newTransport =  {...transport};
                                                            newTransport.parkNumber = event.currentTarget.value;
                                                            setTransport(newTransport);
                                                            //TODO save transport
                                                        }}
                                                        placeholder="0000"
                                                    />
                                                </Table.Td>
                                                <Table.Td h={40}>
                                                    <TextInput
                                                        value={transport.column}
                                                        onChange={(event) => {
                                                            const newTransport =  {...transport};
                                                            newTransport.column = event.currentTarget.value;
                                                            setTransport(newTransport);
                                                            //TODO save transport
                                                        }}
                                                        placeholder="000"
                                                    />
                                                </Table.Td>
                                                <Table.Td h={40}>
                                                    <TextInput
                                                        value={transport.brand}
                                                        onChange={(event) => {
                                                            const newTransport =  {...transport};
                                                            newTransport.brand = event.currentTarget.value;
                                                            setTransport(newTransport);
                                                            //TODO save transport
                                                        }}
                                                        placeholder="Марка"
                                                    />
                                                </Table.Td>
                                                <Table.Td h={40}>
                                                    <TextInput
                                                        value={transport.model}
                                                        onChange={(event) => {
                                                            const newTransport =  {...transport};
                                                            newTransport.model = event.currentTarget.value;
                                                            setTransport(newTransport);
                                                            //TODO save transport
                                                        }}
                                                        placeholder="Модель"
                                                    />
                                                </Table.Td>

                                                <Table.Td h={40}>
                                                    <TextInput
                                                        value={transport.number}
                                                        onChange={(event) => {
                                                            const newTransport =  {...transport};
                                                            newTransport.number = event.currentTarget.value;
                                                            setTransport(newTransport);
                                                            //TODO save transport
                                                        }}
                                                        placeholder=""
                                                    />
                                                </Table.Td>

                                                <Table.Td h={40}>
                                                    <TextInput
                                                        value={transport.vin}
                                                        onChange={(event) => {
                                                            const newTransport =  {...transport};
                                                            newTransport.vin = event.currentTarget.value;
                                                            setTransport(newTransport);
                                                            //TODO save transport
                                                        }}
                                                        placeholder=""
                                                    />
                                                </Table.Td>

                                                <Table.Td w={20}>
                                                    <div>
                                                        {user && user?.role !== UserRole.MASTER && (
                                                            <>
                                                                <div className="mr-4">
                                                                    <Button
                                                                        onClick={() => {

                                                                        }}
                                                                    >
                                                                        Док-ты
                                                                    </Button>
                                                                </div>
                                                            </>
                                                        )}
                                                    </div>
                                                </Table.Td>
                                            </Table.Tr>
                                        );
                                    })}
                                </Table.Tbody>
                            </Table>
                        </div>
                    </div>
                </div>
                <div className="flex flex-col space-y-4">
                    <div className="p2">
                        {user && user?.role !== UserRole.MASTER && (
                            <>
                                <div className="mr-4">
                                    <Button
                                        onClick={() => {

                                        }}
                                    >
                                        Добавить водителя
                                    </Button>
                                </div>
                            </>
                        )}
                    </div>
                    <div className="p2">
                        <Table className="min-w-[400px]" >
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>Водитель</Table.Th>
                                    <Table.Th>Телефон</Table.Th>
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {Array.from(drivers.values()).map((driverState) => {
                                    const [driver, setDriver] = driverState;
                                    return  (
                                        <Table.Tr key={driver.id}>
                                            <Table.Td h={40}>
                                                <TextInput
                                                    value={driver.name}
                                                    onChange={(event) => {
                                                        const newDriver =  {...driver};
                                                        newDriver.name = event.currentTarget.value;
                                                        setDriver(newDriver);
                                                        //TODO save driver
                                                    }}
                                                    placeholder="Иван Иванов"
                                                />
                                            </Table.Td>
                                            <Table.Td h={40}>
                                                <PhoneInput
                                                    phone={driver.phone}
                                                    onChange={(value) => {
                                                        const newDriver =  {...driver};
                                                        newDriver.phone =value;
                                                        setDriver(newDriver);
                                                        //TODO save driver
                                                    }}
                                                />
                                            </Table.Td>
                                        </Table.Tr>
                                    );
                                })}
                            </Table.Tbody>
                        </Table>
                    </div>
                </div>
                <div className="flex flex-col space-y-4">
                    <div className="p2">
                        <div>
                            {user && user?.role !== UserRole.MASTER && (
                                <>
                                    <div className="mr-4">
                                        <Button
                                            onClick={() => {

                                            }}
                                        >
                                            Добавить прицеп
                                        </Button>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                    <div className="p2">
                        <Table className="min-w-[800px]"  >
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>№ в парке</Table.Th>
                                    <Table.Th>№ колонны</Table.Th>
                                    <Table.Th>Марка</Table.Th>
                                    <Table.Th>Модель</Table.Th>
                                    <Table.Th>Гос. №</Table.Th>
                                    <Table.Th>VIN</Table.Th>
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {Array.from(transports.values()).map((transportState) => {
                                    const [transport, setTransport] = transportState;
                                    return  (
                                        <Table.Tr key={transport.id}>
                                            <Table.Td h={40} w={100}>
                                                <TextInput
                                                    value={transport.parkNumber}
                                                    onChange={(event) => {
                                                        const newTransport =  {...transport};
                                                        newTransport.parkNumber = event.currentTarget.value;
                                                        setTransport(newTransport);
                                                        //TODO save transport
                                                    }}
                                                    placeholder="0000"
                                                />
                                            </Table.Td>
                                            <Table.Td h={40}>
                                                <TextInput
                                                    value={transport.column}
                                                    onChange={(event) => {
                                                        const newTransport =  {...transport};
                                                        newTransport.column = event.currentTarget.value;
                                                        setTransport(newTransport);
                                                        //TODO save transport
                                                    }}
                                                    placeholder="000"
                                                />
                                            </Table.Td>
                                            <Table.Td h={40}>
                                                <TextInput
                                                    value={transport.brand}
                                                    onChange={(event) => {
                                                        const newTransport =  {...transport};
                                                        newTransport.brand = event.currentTarget.value;
                                                        setTransport(newTransport);
                                                        //TODO save transport
                                                    }}
                                                    placeholder="Марка"
                                                />
                                            </Table.Td>
                                            <Table.Td h={40}>
                                                <TextInput
                                                    value={transport.model}
                                                    onChange={(event) => {
                                                        const newTransport =  {...transport};
                                                        newTransport.model = event.currentTarget.value;
                                                        setTransport(newTransport);
                                                        //TODO save transport
                                                    }}
                                                    placeholder="Модель"
                                                />
                                            </Table.Td>

                                            <Table.Td h={40}>
                                                <TextInput
                                                    value={transport.vin}
                                                    onChange={(event) => {
                                                        const newTransport =  {...transport};
                                                        newTransport.vin = event.currentTarget.value;
                                                        setTransport(newTransport);
                                                        //TODO save transport
                                                    }}
                                                    placeholder=""
                                                />
                                            </Table.Td>
                                        </Table.Tr>
                                    );
                                })}
                            </Table.Tbody>
                        </Table>
                    </div>

                </div>
            </div>
        </div>
    );
}
