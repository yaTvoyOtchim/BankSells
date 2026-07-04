package com.example.vtbsales.data

import com.example.vtbsales.model.Plan
import com.example.vtbsales.model.ClientSession
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.model.Role
import com.example.vtbsales.model.Sale
import com.example.vtbsales.model.SalesSeed
import com.example.vtbsales.model.TeamLink
import com.example.vtbsales.model.User
import com.example.vtbsales.model.Office
import java.time.LocalDate

object DemoData {
    fun seed(today: LocalDate = LocalDate.now()): SalesSeed {
        val officePrimary = Office(
            id = "office-8617-0290",
            title = "Доп. офис №8617/0290",
            city = "Симферополь",
            region = "Крым"
        )
        val officeSecond = Office(
            id = "office-8617-0331",
            title = "Доп. офис №8617/0331",
            city = "Севастополь",
            region = "Крым"
        )
        val admin = User(
            id = "admin-1",
            uid = "VTB-ADMIN-ALL",
            name = "Администратор сети",
            role = Role.Admin,
            office = "Все офисы",
            pinHashDemo = "9999",
            createdAt = today.minusMonths(3),
            officeId = "all"
        )
        val manager = User(
            id = "manager-1",
            uid = "VTB-MGR-8617",
            name = "Петров Игорь Сергеевич",
            role = Role.Manager,
            office = officePrimary.title,
            pinHashDemo = "0000",
            createdAt = today.minusMonths(2),
            officeId = officePrimary.id
        )
        val managerSecond = User(
            id = "manager-2",
            uid = "VTB-MGR-0331",
            name = "Соколова Марина Андреевна",
            role = Role.Manager,
            office = officeSecond.title,
            pinHashDemo = "4444",
            createdAt = today.minusMonths(2),
            officeId = officeSecond.id
        )
        val nikita = User(
            id = "employee-1",
            uid = "VTB-195-204",
            name = "Якименко Никита Дмитриевич",
            role = Role.Employee,
            office = officePrimary.title,
            pinHashDemo = "1111",
            createdAt = today.minusMonths(1),
            officeId = officePrimary.id
        )
        val anna = User(
            id = "employee-2",
            uid = "VTB-231-771",
            name = "Анна Петрова",
            role = Role.Employee,
            office = officePrimary.title,
            pinHashDemo = "2222",
            createdAt = today.minusMonths(1),
            officeId = officePrimary.id
        )
        val oleg = User(
            id = "employee-3",
            uid = "VTB-144-880",
            name = "Олег Смирнов",
            role = Role.Employee,
            office = officePrimary.title,
            pinHashDemo = "3333",
            createdAt = today.minusMonths(1),
            officeId = officePrimary.id
        )
        val irina = User(
            id = "employee-4",
            uid = "VTB-566-120",
            name = "Ирина Волкова",
            role = Role.Employee,
            office = officeSecond.title,
            pinHashDemo = "5555",
            createdAt = today.minusMonths(1),
            officeId = officeSecond.id
        )

        val clientSessions = listOf(
            ClientSession("client-demo-1", nikita.id, "1234", 1, today),
            ClientSession("client-demo-2", nikita.id, "8842", 1, today),
            ClientSession("client-demo-3", irina.id, "6610", 1, today)
        )

        val sales = listOf(
            Sale("s1", nikita.id, ProductType.DebitCard, "ДК/стик по заявке", 2, 0.0, 50.0, 0, today, "client-demo-1"),
            Sale("s2", nikita.id, ProductType.CreditCard, "КК по заявке", 1, 0.0, 30.0, 0, today, "client-demo-1"),
            Sale("s3", nikita.id, ProductType.Deposit, "Накопительный счет", 1, 0.0, 30.0, 0, today, "client-demo-2"),
            Sale("s4", nikita.id, ProductType.Insurance, "СОЦ. Выплаты", 1, 0.0, 35.0, 0, today, "client-demo-2"),
            Sale("s5", nikita.id, ProductType.SimCard, "Карта + СМС", 1, 0.0, 20.0, 0, today, "client-demo-2"),
            Sale("s6", nikita.id, ProductType.SalaryProject, "ЗП Лайт", 1, 0.0, 30.0, 0, today, "client-demo-2"),
            Sale("m1", nikita.id, ProductType.DebitCard, "ДК/стик по заявке", 47, 0.0, 1175.0, 20, today.minusDays(10)),
            Sale("m2", nikita.id, ProductType.Deposit, "Накопительный счет", 7, 0.0, 210.0, 4, today.minusDays(9)),
            Sale("m3", nikita.id, ProductType.CreditCard, "КСП", 1, 1350.0, 80.0, 1, today.minusDays(7)),
            Sale("m4", nikita.id, ProductType.Other, "Автостягивание", 1, 0.0, 25.0, 0, today.minusDays(5)),
            Sale("m5", nikita.id, ProductType.Other, "ДК продажа доп. карты", 18, 0.0, 360.0, 6, today.minusDays(3)),
            Sale("a1", anna.id, ProductType.DebitCard, "Дебетовая карта", 5, 0.0, 120.0, 2, today),
            Sale("a2", anna.id, ProductType.CreditCard, "Кредитная карта", 3, 0.0, 120.0, 1, today),
            Sale("a3", anna.id, ProductType.Deposit, "Вклад", 3, 750000.0, 180.0, 2, today),
            Sale("o1", oleg.id, ProductType.DebitCard, "Дебетовая карта", 1, 0.0, 20.0, 1, today.minusDays(1)),
            Sale("i1", irina.id, ProductType.CreditCard, "Кредитная карта", 2, 0.0, 70.0, 0, today, "client-demo-3"),
            Sale("i2", irina.id, ProductType.Insurance, "Страхование", 1, 0.0, 35.0, 0, today, "client-demo-3")
        )

        return SalesSeed(
            users = listOf(admin, manager, managerSecond, nikita, anna, oleg, irina),
            sales = sales,
            teamLinks = listOf(
                TeamLink(manager.id, nikita.id, manager.office),
                TeamLink(manager.id, anna.id, manager.office),
                TeamLink(manager.id, oleg.id, manager.office),
                TeamLink(managerSecond.id, irina.id, managerSecond.office)
            ),
            plans = listOf(
                Plan(manager.office, "Июль 2026", pointsTarget = 7800.0, clientsTarget = 52),
                Plan(managerSecond.office, "Июль 2026", pointsTarget = 7200.0, clientsTarget = 48)
            ),
            clientSessions = clientSessions,
            offices = listOf(officePrimary, officeSecond)
        )
    }
}
