"use client";
import { Menu } from "../../features/menu/presentation/Menu";
import "moment/locale/ru";
import {
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY,
} from "../../constants";
import UserApiRepository from "../../features/user/data/UserApiRepository";
import ApiHelper from "../../util/api/ApiHelper";
import LoadingComponent from "../../util/components/LoadingComponent";
import UserStatisticsApiRepository from "@/features/user/statistics/data/UserStatisticsApiRepository";
import { useEffect, useState, useRef, useLayoutEffect } from "react";
import { UserStatisticsPeriod } from "@/features/user/statistics/domain/UserStatisticsPeriod";
import { RegistrationDto } from "@/features/user/statistics/domain/RegistrationDto";
import * as am5 from "@amcharts/amcharts5";
import * as am5xy from "@amcharts/amcharts5/xy";
import am5themes_Animated from "@amcharts/amcharts5/themes/Animated";
import OrderStatisticsApiRepository from "@/features/orders/orders/statistics/data/OrderStatisticsApiRepository";
import { OrderCreationDto } from "@/features/orders/orders/statistics/domain/OrderCreationDto";
import { OrderStatisticsPeriod } from "@/features/orders/orders/statistics/domain/OrderStatisticsPeriod";
import { Select } from "@mantine/core";

const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);
const userStatisticsRepository = new UserStatisticsApiRepository(
  apiHelper,
  userApiRepository
);
const orderStatisticsApiRepository = new OrderStatisticsApiRepository(
  apiHelper,
  userApiRepository
);

export default function Page() {
  const [statisticsPeriod, setStatisticsPeriod] =
    useState<UserStatisticsPeriod>(UserStatisticsPeriod.MONTH);

  const [isLoadingUsersStatistics, setLoadingUsersStatistics] = useState(true);
  const [usersStatistics, setUsersStatistics] = useState<RegistrationDto[]>([]);

  const [isLoadingOrdersStatistics, setLoadingOrdersStatistics] =
    useState(true);
  const [ordersStatistics, setOrdersStatistics] = useState<OrderCreationDto[]>(
    []
  );

  const chartRef = useRef<HTMLDivElement>(null);
  const ordersChartRef = useRef<HTMLDivElement>(null);

  const loadUsersStatistics = async () => {
    setLoadingUsersStatistics(true);

    try {
      const registrationsStatistics =
        await userStatisticsRepository.getRegistrationsStatistics(
          statisticsPeriod
        );
      setUsersStatistics(registrationsStatistics);
    } catch (e) {
      alert((e as Error).message);
    }

    setLoadingUsersStatistics(false);
  };

  const loadOrdersStatistics = async () => {
    setLoadingOrdersStatistics(true);

    try {
      const ordersStatistics =
        await orderStatisticsApiRepository.getOrdersStatistics(
          statisticsPeriod
        );
      setOrdersStatistics(ordersStatistics);
    } catch (e) {
      alert((e as Error).message);
    }

    setLoadingOrdersStatistics(false);
  };

  useEffect(() => {
    loadUsersStatistics();
    loadOrdersStatistics();
  }, [statisticsPeriod]);

  useLayoutEffect(() => {
    if (isLoadingUsersStatistics || !chartRef.current) return;

    let root = am5.Root.new(chartRef.current);

    root.setThemes([am5themes_Animated.new(root)]);

    let chart = root.container.children.push(
      am5xy.XYChart.new(root, {
        panX: true,
        panY: false,
        wheelX: "panX",
        wheelY: "zoomX",
        layout: root.verticalLayout,
      })
    );

    // Set Data
    let data = usersStatistics.map((item) => ({
      date: new Date(item.date).getTime(),
      count: item.count,
    }));

    // Determine the maximum Y value
    let maxDataValue = Math.max(...data.map((item) => item.count));
    let maxYValue = Math.max(100, maxDataValue);

    // Create Y-Axis (ValueAxis) with min and max
    let yAxis = chart.yAxes.push(
      am5xy.ValueAxis.new(root, {
        min: 0,
        max: maxYValue,
        strictMinMax: true,
        renderer: am5xy.AxisRendererY.new(root, {}),
      })
    );

    // Create X-Axis (DateAxis)
    let xAxis = chart.xAxes.push(
      am5xy.DateAxis.new(root, {
        baseInterval: {
          timeUnit: "day",
          count: 1,
        },
        renderer: am5xy.AxisRendererX.new(root, {}),
        tooltip: am5.Tooltip.new(root, {}),
      })
    );

    // Create Series (ColumnSeries)
    let series = chart.series.push(
      am5xy.ColumnSeries.new(root, {
        name: "Registrations",
        xAxis: xAxis,
        yAxis: yAxis,
        valueYField: "count",
        valueXField: "date",
        tooltip: am5.Tooltip.new(root, {
          labelText:
            statisticsPeriod === UserStatisticsPeriod.MONTH
              ? "{valueX.formatDate('yyyy-MM')} - {valueY}"
              : "{valueX.formatDate('yyyy-MM-dd')} - {valueY}",
        }),
      })
    );

    // **Set the color of the columns to #0d6efd**
    series.columns.template.setAll({
      fill: am5.color("#0d6efd"),
      stroke: am5.color("#0d6efd"),
    });

    series.data.setAll(data);

    // Add Cursor
    chart.set("cursor", am5xy.XYCursor.new(root, {}));

    // Animate on Load
    series.appear(1000);
    chart.appear(1000, 100);

    // Cleanup on unmount
    return () => {
      root.dispose();
    };
  }, [isLoadingUsersStatistics, usersStatistics]);

  // New useLayoutEffect for Orders Statistics Chart
  useLayoutEffect(() => {
    if (isLoadingOrdersStatistics || !ordersChartRef.current) return;

    let root = am5.Root.new(ordersChartRef.current);

    root.setThemes([am5themes_Animated.new(root)]);

    let chart = root.container.children.push(
      am5xy.XYChart.new(root, {
        panX: true,
        panY: false,
        wheelX: "panX",
        wheelY: "zoomX",
        layout: root.verticalLayout,
      })
    );

    // Set Data
    let data = ordersStatistics.map((item) => ({
      date: new Date(item.date).getTime(),
      count: item.count,
    }));

    // Determine the maximum Y value
    let maxDataValue = Math.max(...data.map((item) => item.count));
    let maxYValue = Math.max(100, maxDataValue);

    // Create Y-Axis (ValueAxis) with min and max
    let yAxis = chart.yAxes.push(
      am5xy.ValueAxis.new(root, {
        min: 0,
        max: maxYValue,
        strictMinMax: true,
        renderer: am5xy.AxisRendererY.new(root, {}),
      })
    );

    // Create X-Axis (DateAxis)
    let xAxis = chart.xAxes.push(
      am5xy.DateAxis.new(root, {
        baseInterval: {
          timeUnit: "day",
          count: 1,
        },
        renderer: am5xy.AxisRendererX.new(root, {}),
        tooltip: am5.Tooltip.new(root, {}),
      })
    );

    // Create Series (ColumnSeries)
    let series = chart.series.push(
      am5xy.ColumnSeries.new(root, {
        name: "Orders",
        xAxis: xAxis,
        yAxis: yAxis,
        valueYField: "count",
        valueXField: "date",
        tooltip: am5.Tooltip.new(root, {
          labelText:
            statisticsPeriod === UserStatisticsPeriod.MONTH
              ? "{valueX.formatDate('yyyy-MM')} - {valueY}"
              : "{valueX.formatDate('yyyy-MM-dd')} - {valueY}",
        }),
      })
    );

    // **Set the color of the columns to #0d6efd**
    series.columns.template.setAll({
      fill: am5.color("#0d6efd"),
      stroke: am5.color("#0d6efd"),
    });

    series.data.setAll(data);

    // Add Cursor
    chart.set("cursor", am5xy.XYCursor.new(root, {}));

    // Animate on Load
    series.appear(1000);
    chart.appear(1000, 100);

    // Cleanup on unmount
    return () => {
      root.dispose();
    };
  }, [isLoadingOrdersStatistics, ordersStatistics]);

  return (
    <Menu userApiRepository={userApiRepository}>
      <main className="mb-20 mt-6">
        <div className="mb-5">
          <Select
            value={statisticsPeriod}
            onChange={(e) => setStatisticsPeriod(e as UserStatisticsPeriod)}
            data={[
              { value: UserStatisticsPeriod.DAY, label: "День" },
              { value: UserStatisticsPeriod.WEEK, label: "Неделя" },
              { value: UserStatisticsPeriod.MONTH, label: "Месяц" },
            ]}
            className="w-[200px]"
          />
        </div>

        <div className="text-lg font-bold mb-5">Статистика регистраций</div>

        {isLoadingUsersStatistics ? (
          <LoadingComponent />
        ) : (
          // Users Statistics Chart
          <div
            id="chartdiv"
            ref={chartRef}
            style={{ width: "100%", height: "300px" }}
          ></div>
        )}

        <div className="text-lg font-bold mb-5 mt-10">Статистика заказов</div>

        {isLoadingOrdersStatistics ? (
          <LoadingComponent />
        ) : (
          // Orders Statistics Chart
          <div
            id="ordersChartDiv"
            ref={ordersChartRef}
            style={{ width: "100%", height: "300px" }}
          ></div>
        )}
      </main>
    </Menu>
  );
}
