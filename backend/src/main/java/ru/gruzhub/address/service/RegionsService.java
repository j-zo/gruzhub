package ru.gruzhub.address.service;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.address.models.Country;
import ru.gruzhub.address.models.Region;
import ru.gruzhub.address.repositories.CountryRepository;
import ru.gruzhub.address.repositories.RegionRepository;

@Service
@RequiredArgsConstructor
public class RegionsService {
    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;

    private boolean isInitialized = false;

    @PostConstruct
    public void initialize() {
        this.createAllCountriesIfNeeded();
        this.createAllRegionsIfNeeded();
        this.isInitialized = true;
    }

    public List<Region> getRegions() {
        return this.regionRepository.findAll();
    }

    public Region getRegionById(Long regionId) {
        if (!this.isInitialized) {
            this.initialize();
        }

        return this.regionRepository.findById(regionId)
                                    .orElseThrow(() -> new NotFoundException("Region with ID " +
                                                                             regionId +
                                                                             " not found"));
    }

    public List<Country> getCountries() {
        if (!this.isInitialized) {
            this.initialize();
        }

        return this.countryRepository.findAll()
                                     .stream()
                                     .filter(country -> Arrays.asList("RU",
                                                                      "LV",
                                                                      "LT",
                                                                      "KZ",
                                                                      "KG",
                                                                      "BY")
                                                              .contains(country.getCode()))
                                     .collect(Collectors.toList());
    }

    private void createAllRegionsIfNeeded() {
        if (this.regionRepository.count() > 0) {
            return;
        }

        for (String[] regionData : this.getRegionsList()) {
            this.createRegion(regionData[0], regionData[1], regionData[2]);
        }
    }

    private void createRegion(String regionName, String regionCodes, String countryCode) {
        Optional<Region> existingRegion = this.regionRepository.findByName(regionName);
        if (existingRegion.isPresent()) {
            return;
        }

        Country country = this.countryRepository.findById(countryCode)
                                                .orElseThrow(() -> new NotFoundException(
                                                    "Country with code " +
                                                    countryCode +
                                                    " not found"));

        Region region = new Region();
        region.setName(regionName);
        region.setCodes(regionCodes);
        region.setCountry(country);

        this.regionRepository.save(region);
    }

    private void createAllCountriesIfNeeded() {
        if (this.countryRepository.count() > 0) {
            return;
        }

        for (String[] countryData : this.getCountriesList()) {
            this.createCountry(countryData[0], countryData[1]);
        }
    }

    private void createCountry(String countryName, String countryCode) {
        Optional<Country> existingCountry = this.countryRepository.findByName(countryName);
        if (existingCountry.isPresent()) {
            return;
        }

        Country country = new Country();
        country.setName(countryName);
        country.setCode(countryCode);

        this.countryRepository.save(country);
    }

    private List<String[]> getRegionsList() {
        return Arrays.asList(new String[] {"Адыгея", "1", "RU"},
                             new String[] {"Башкортостан", "2,102", "RU"},
                             new String[] {"Бурятия", "3", "RU"},
                             new String[] {"Алтай (Горный Алтай)", "4", "RU"},
                             new String[] {"Дагестан", "5", "RU"},
                             new String[] {"Ингушетия", "6", "RU"},
                             new String[] {"Кабардино-Балкария", "7", "RU"},
                             new String[] {"Калмыкия", "8", "RU"},
                             new String[] {"Карачаево-Черкессия", "9", "RU"},
                             new String[] {"Карелия", "10", "RU"},
                             new String[] {"Коми", "11", "RU"},
                             new String[] {"Марий Эл", "12", "RU"},
                             new String[] {"Мордовия", "13,113", "RU"},
                             new String[] {"Саха (Якутия)", "14", "RU"},
                             new String[] {"Северная Осетия — Алания", "15", "RU"},
                             new String[] {"Татарстан", "16,116,716", "RU"},
                             new String[] {"Тыва", "17", "RU"},
                             new String[] {"Удмуртия", "18", "RU"},
                             new String[] {"Хакасия", "19", "RU"},
                             new String[] {"Чувашия", "21,121", "RU"},
                             new String[] {"Алтайский край", "22", "RU"},
                             new String[] {"Краснодарский край", "23,93,123", "RU"},
                             new String[] {"Красноярский край", "24,84,88,124", "RU"},
                             new String[] {"Приморский край", "25,125", "RU"},
                             new String[] {"Ставропольский край", "26,126", "RU"},
                             new String[] {"Хабаровский край", "27", "RU"},
                             new String[] {"Амурская область", "28", "RU"},
                             new String[] {"Архангельская область", "29", "RU"},
                             new String[] {"Астраханская область", "30", "RU"},
                             new String[] {"Белгородская область", "31", "RU"},
                             new String[] {"Брянская область", "32", "RU"},
                             new String[] {"Владимирская область", "33", "RU"},
                             new String[] {"Волгоградская область", "34,134", "RU"},
                             new String[] {"Вологодская область", "35", "RU"},
                             new String[] {"Воронежская область", "36,136", "RU"},
                             new String[] {"Ивановская область", "37", "RU"},
                             new String[] {"Иркутская область", "38,85,138", "RU"},
                             new String[] {"Калининградская область", "39,91", "RU"},
                             new String[] {"Калужская область", "40", "RU"},
                             new String[] {"Камчатский край", "41", "RU"},
                             new String[] {"Кемеровская область", "42,142", "RU"},
                             new String[] {"Кировская область", "43", "RU"},
                             new String[] {"Костромская область", "44", "RU"},
                             new String[] {"Курганская область", "45", "RU"},
                             new String[] {"Курская область", "46", "RU"},
                             new String[] {"Ленинградская область", "47", "RU"},
                             new String[] {"Липецкая область", "48", "RU"},
                             new String[] {"Магаданская область", "49", "RU"},
                             new String[] {"Московская область", "50,90,150,190,750", "RU"},
                             new String[] {"Мурманская область", "51", "RU"},
                             new String[] {"Нижегородская область", "52,152", "RU"},
                             new String[] {"Новгородская область", "53", "RU"},
                             new String[] {"Новосибирская область", "54,154", "RU"},
                             new String[] {"Омская область", "55", "RU"},
                             new String[] {"Оренбургская область", "56", "RU"},
                             new String[] {"Орловская область", "57", "RU"},
                             new String[] {"Пензенская область", "58", "RU"},
                             new String[] {"Пермский край", "59,81,159", "RU"},
                             new String[] {"Псковская область", "60", "RU"},
                             new String[] {"Ростовская область", "61,161", "RU"},
                             new String[] {"Рязанская область", "62", "RU"},
                             new String[] {"Самарская область", "63,163", "RU"},
                             new String[] {"Саратовская область", "64,164", "RU"},
                             new String[] {"Сахалинская область", "65", "RU"},
                             new String[] {"Свердловская область", "66,96,196", "RU"},
                             new String[] {"Смоленская область", "67", "RU"},
                             new String[] {"Тамбовская область", "68", "RU"},
                             new String[] {"Тверская область", "69", "RU"},
                             new String[] {"Томская область", "70", "RU"},
                             new String[] {"Тульская область", "71", "RU"},
                             new String[] {"Тюменская область", "72", "RU"},
                             new String[] {"Ульяновская область", "73,173", "RU"},
                             new String[] {"Челябинская область", "74,174", "RU"},
                             new String[] {"Забайкальский край", "75,80", "RU"},
                             new String[] {"Ярославская область", "76", "RU"},
                             new String[] {"Москва", "77,97,99,177,197,199,777,799,977", "RU"},
                             new String[] {"Санкт-Петербург", "78,98,178", "RU"},
                             new String[] {"Еврейская автономная область", "79", "RU"},
                             new String[] {"Крым", "82", "RU"},
                             new String[] {"Ненецкий автономный округ", "83", "RU"},
                             new String[] {"Ханты-Мансийский автономный округ — Югра",
                                           "86,186",
                                           "RU"},
                             new String[] {"Чукотский автономный округ", "87", "RU"},
                             new String[] {"Ямало-Ненецкий автономный округ", "89", "RU"},
                             new String[] {"Севастополь", "92", "RU"},
                             new String[] {
                                 "Территории за пределами РФ, обслуживаемые Департаментом " +
                                 "режимных объектов МВД России",
                                 "94",
                                 "RU"},
                             new String[] {"Чечня", "95", "RU"},
                             new String[] {"Рига", "-", "LV"},
                             new String[] {"Вильнюс", "-", "LT"},
                             new String[] {"Астана", "-", "KZ"},
                             new String[] {"Бишкек", "-", "KG"},
                             new String[] {"Минск", "-", "BY"});
    }

    private List<String[]> getCountriesList() {
        return Arrays.asList(new String[] {"Абхазия", "AB"},
                             new String[] {"Австралия", "AU"},
                             new String[] {"Австрия", "AT"},
                             new String[] {"Азербайджан", "AZ"},
                             new String[] {"Албания", "AL"},
                             new String[] {"Алжир", "DZ"},
                             new String[] {"Американское Самоа", "AS"},
                             new String[] {"Ангилья", "AI"},
                             new String[] {"Ангола", "AO"},
                             new String[] {"Андорра", "AD"},
                             new String[] {"Антарктида", "AQ"},
                             new String[] {"Антигуа и Барбуда", "AG"},
                             new String[] {"Аргентина", "AR"},
                             new String[] {"Армения", "AM"},
                             new String[] {"Аруба", "AW"},
                             new String[] {"Афганистан", "AF"},
                             new String[] {"Багамы", "BS"},
                             new String[] {"Бангладеш", "BD"},
                             new String[] {"Барбадос", "BB"},
                             new String[] {"Бахрейн", "BH"},
                             new String[] {"Беларусь", "BY"},
                             new String[] {"Белиз", "BZ"},
                             new String[] {"Бельгия", "BE"},
                             new String[] {"Бенин", "BJ"},
                             new String[] {"Бермуды", "BM"},
                             new String[] {"Болгария", "BG"},
                             new String[] {"Боливия, Многонациональное Государство", "BO"},
                             new String[] {"Бонайре, Саба и Синт-Эстатиус", "BQ"},
                             new String[] {"Босния и Герцеговина", "BA"},
                             new String[] {"Ботсвана", "BW"},
                             new String[] {"Бразилия", "BR"},
                             new String[] {"Британская территория в Индийском океане", "IO"},
                             new String[] {"Бруней-Даруссалам", "BN"},
                             new String[] {"Буркина-Фасо", "BF"},
                             new String[] {"Бурунди", "BI"},
                             new String[] {"Бутан", "BT"},
                             new String[] {"Вануату", "VU"},
                             new String[] {"Венгрия", "HU"},
                             new String[] {"Венесуэла Боливарианская Республика", "VE"},
                             new String[] {"Виргинские острова, Британские", "VG"},
                             new String[] {"Виргинские острова, США", "VI"},
                             new String[] {"Вьетнам", "VN"},
                             new String[] {"Габон", "GA"},
                             new String[] {"Гаити", "HT"},
                             new String[] {"Гайана", "GY"},
                             new String[] {"Гамбия", "GM"},
                             new String[] {"Гана", "GH"},
                             new String[] {"Гваделупа", "GP"},
                             new String[] {"Гватемала", "GT"},
                             new String[] {"Гвинея", "GN"},
                             new String[] {"Гвинея-Бисау", "GW"},
                             new String[] {"Германия", "DE"},
                             new String[] {"Гернси", "GG"},
                             new String[] {"Гибралтар", "GI"},
                             new String[] {"Гондурас", "HN"},
                             new String[] {"Гонконг", "HK"},
                             new String[] {"Гренада", "GD"},
                             new String[] {"Гренландия", "GL"},
                             new String[] {"Греция", "GR"},
                             new String[] {"Грузия", "GE"},
                             new String[] {"Гуам", "GU"},
                             new String[] {"Дания", "DK"},
                             new String[] {"Джерси", "JE"},
                             new String[] {"Джибути", "DJ"},
                             new String[] {"Доминика", "DM"},
                             new String[] {"Доминиканская Республика", "DO"},
                             new String[] {"Египет", "EG"},
                             new String[] {"Замбия", "ZM"},
                             new String[] {"Западная Сахара", "EH"},
                             new String[] {"Зимбабве", "ZW"},
                             new String[] {"Израиль", "IL"},
                             new String[] {"Индия", "IN"},
                             new String[] {"Индонезия", "ID"},
                             new String[] {"Иордания", "JO"},
                             new String[] {"Ирак", "IQ"},
                             new String[] {"Иран, Исламская Республика", "IR"},
                             new String[] {"Ирландия", "IE"},
                             new String[] {"Исландия", "IS"},
                             new String[] {"Испания", "ES"},
                             new String[] {"Италия", "IT"},
                             new String[] {"Йемен", "YE"},
                             new String[] {"Кабо-Верде", "CV"},
                             new String[] {"Казахстан", "KZ"},
                             new String[] {"Камбоджа", "KH"},
                             new String[] {"Камерун", "CM"},
                             new String[] {"Канада", "CA"},
                             new String[] {"Катар", "QA"},
                             new String[] {"Кения", "KE"},
                             new String[] {"Кипр", "CY"},
                             new String[] {"Киргизия", "KG"},
                             new String[] {"Кирибати", "KI"},
                             new String[] {"Китай", "CN"},
                             new String[] {"Кокосовые (Килинг) острова", "CC"},
                             new String[] {"Колумбия", "CO"},
                             new String[] {"Коморы", "KM"},
                             new String[] {"Конго", "CG"},
                             new String[] {"Конго, Демократическая Республика", "CD"},
                             new String[] {"Корея, Народно-Демократическая Республика", "KP"},
                             new String[] {"Корея, Республика", "KR"},
                             new String[] {"Коста-Рика", "CR"},
                             new String[] {"Кот д'Ивуар", "CI"},
                             new String[] {"Куба", "CU"},
                             new String[] {"Кувейт", "KW"},
                             new String[] {"Кюрасао", "CW"},
                             new String[] {"Лаос", "LA"},
                             new String[] {"Латвия", "LV"},
                             new String[] {"Лесото", "LS"},
                             new String[] {"Ливан", "LB"},
                             new String[] {"Ливийская Арабская Джамахирия", "LY"},
                             new String[] {"Либерия", "LR"},
                             new String[] {"Лихтенштейн", "LI"},
                             new String[] {"Литва", "LT"},
                             new String[] {"Люксембург", "LU"},
                             new String[] {"Маврикий", "MU"},
                             new String[] {"Мавритания", "MR"},
                             new String[] {"Мадагаскар", "MG"},
                             new String[] {"Майотта", "YT"},
                             new String[] {"Макао", "MO"},
                             new String[] {"Малави", "MW"},
                             new String[] {"Малайзия", "MY"},
                             new String[] {"Мали", "ML"},
                             new String[] {
                                 "Малые Тихоокеанские отдаленные острова Соединенных Штатов",
                                 "UM"},
                             new String[] {"Мальдивы", "MV"},
                             new String[] {"Мальта", "MT"},
                             new String[] {"Марокко", "MA"},
                             new String[] {"Мартиника", "MQ"},
                             new String[] {"Маршалловы острова", "MH"},
                             new String[] {"Мексика", "MX"},
                             new String[] {"Микронезия, Федеративные Штаты", "FM"},
                             new String[] {"Мозамбик", "MZ"},
                             new String[] {"Молдова, Республика", "MD"},
                             new String[] {"Монако", "MC"},
                             new String[] {"Монголия", "MN"},
                             new String[] {"Монтсеррат", "MS"},
                             new String[] {"Мьянма", "MM"},
                             new String[] {"Намибия", "NA"},
                             new String[] {"Науру", "NR"},
                             new String[] {"Непал", "NP"},
                             new String[] {"Нигер", "NE"},
                             new String[] {"Нигерия", "NG"},
                             new String[] {"Нидерланды", "NL"},
                             new String[] {"Никарагуа", "NI"},
                             new String[] {"Ниуэ", "NU"},
                             new String[] {"Новая Зеландия", "NZ"},
                             new String[] {"Новая Каледония", "NC"},
                             new String[] {"Норвегия", "NO"},
                             new String[] {"Объединенные Арабские Эмираты", "AE"},
                             new String[] {"Оман", "OM"},
                             new String[] {"Остров Буве", "BV"},
                             new String[] {"Остров Мэн", "IM"},
                             new String[] {"Остров Норфолк", "NF"},
                             new String[] {"Остров Рождества", "CX"},
                             new String[] {"Остров Херд и острова Макдональд", "HM"},
                             new String[] {"Острова Кайман", "KY"},
                             new String[] {"Острова Кука", "CK"},
                             new String[] {"Острова Теркс и Кайкос", "TC"},
                             new String[] {"Пакистан", "PK"},
                             new String[] {"Палау", "PW"},
                             new String[] {"Палестинская территория, оккупированная", "PS"},
                             new String[] {"Панама", "PA"},
                             new String[] {"Папский Престол (Государство — город Ватикан)", "VA"},
                             new String[] {"Папуа-Новая Гвинея", "PG"},
                             new String[] {"Парагвай", "PY"},
                             new String[] {"Перу", "PE"},
                             new String[] {"Питкерн", "PN"},
                             new String[] {"Польша", "PL"},
                             new String[] {"Португалия", "PT"},
                             new String[] {"Пуэрто-Рико", "PR"},
                             new String[] {"Республика Македония", "MK"},
                             new String[] {"Реюньон", "RE"},
                             new String[] {"Россия", "RU"},
                             new String[] {"Руанда", "RW"},
                             new String[] {"Румыния", "RO"},
                             new String[] {"Самоа", "WS"},
                             new String[] {"Сан-Марино", "SM"},
                             new String[] {"Сан-Томе и Принсипи", "ST"},
                             new String[] {"Саудовская Аравия", "SA"},
                             new String[] {"Святая Елена, Остров вознесения, Тристан-да-Кунья",
                                           "SH"},
                             new String[] {"Северные Марианские острова", "MP"},
                             new String[] {"Сен-Бартельми", "BL"},
                             new String[] {"Сен-Мартен", "MF"},
                             new String[] {"Сенегал", "SN"},
                             new String[] {"Сент-Винсент и Гренадины", "VC"},
                             new String[] {"Сент-Китс и Невис", "KN"},
                             new String[] {"Сент-Люсия", "LC"},
                             new String[] {"Сент-Пьер и Микелон", "PM"},
                             new String[] {"Сербия", "RS"},
                             new String[] {"Сейшелы", "SC"},
                             new String[] {"Сингапур", "SG"},
                             new String[] {"Синт-Мартен", "SX"},
                             new String[] {"Сирийская Арабская Республика", "SY"},
                             new String[] {"Словакия", "SK"},
                             new String[] {"Словения", "SI"},
                             new String[] {"Соединенное Королевство", "GB"},
                             new String[] {"Соединенные Штаты", "US"},
                             new String[] {"Соломоновы острова", "SB"},
                             new String[] {"Сомали", "SO"},
                             new String[] {"Судан", "SD"},
                             new String[] {"Суринам", "SR"},
                             new String[] {"Сьерра-Леоне", "SL"},
                             new String[] {"Таджикистан", "TJ"},
                             new String[] {"Таиланд", "TH"},
                             new String[] {"Тайвань (Китай)", "TW"},
                             new String[] {"Танзания, Объединенная Республика", "TZ"},
                             new String[] {"Тимор-Лесте", "TL"},
                             new String[] {"Того", "TG"},
                             new String[] {"Токелау", "TK"},
                             new String[] {"Тонга", "TO"},
                             new String[] {"Тринидад и Тобаго", "TT"},
                             new String[] {"Тувалу", "TV"},
                             new String[] {"Тунис", "TN"},
                             new String[] {"Туркмения", "TM"},
                             new String[] {"Турция", "TR"},
                             new String[] {"Уганда", "UG"},
                             new String[] {"Узбекистан", "UZ"},
                             new String[] {"Украина", "UA"},
                             new String[] {"Уоллис и Футуна", "WF"},
                             new String[] {"Уругвай", "UY"},
                             new String[] {"Фарерские острова", "FO"},
                             new String[] {"Фиджи", "FJ"},
                             new String[] {"Филиппины", "PH"},
                             new String[] {"Финляндия", "FI"},
                             new String[] {"Фолклендские острова (Мальвинские)", "FK"},
                             new String[] {"Франция", "FR"},
                             new String[] {"Французская Гвиана", "GF"},
                             new String[] {"Французская Полинезия", "PF"},
                             new String[] {"Французские Южные территории", "TF"},
                             new String[] {"Хорватия", "HR"},
                             new String[] {"Центрально-Африканская Республика", "CF"},
                             new String[] {"Чад", "TD"},
                             new String[] {"Черногория", "ME"},
                             new String[] {"Чешская Республика", "CZ"},
                             new String[] {"Чили", "CL"},
                             new String[] {"Швейцария", "CH"},
                             new String[] {"Швеция", "SE"},
                             new String[] {"Шпицберген и Ян Майен", "SJ"},
                             new String[] {"Шри-Ланка", "LK"},
                             new String[] {"Эквадор", "EC"},
                             new String[] {"Экваториальная Гвинея", "GQ"},
                             new String[] {"Эландские острова", "AX"},
                             new String[] {"Эль-Сальвадор", "SV"},
                             new String[] {"Эритрея", "ER"},
                             new String[] {"Эсватини", "SZ"},
                             new String[] {"Эстония", "EE"},
                             new String[] {"Эфиопия", "ET"},
                             new String[] {"Южная Африка", "ZA"},
                             new String[] {"Южная Джорджия и Южные Сандвичевы острова", "GS"},
                             new String[] {"Южная Осетия", "OS"},
                             new String[] {"Южный Судан", "SS"},
                             new String[] {"Ямайка", "JM"},
                             new String[] {"Япония", "JP"});
    }
}
