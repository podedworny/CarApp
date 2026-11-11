# Aplikacja do zarządzania historią serwisową i monitorowania stanu technicznego samochodu

## Opis projektu

Aplikacja umożliwiająca prowadzenie cyfrowej historii serwisowej pojazdu, obejmującej naprawy, wymiany części eksploatacyjnych, przeglądy techniczne oraz koszty utrzymania samochodu. System będzie oferował przypomnienia o zbliżających się czynnościach serwisowych na podstawie dat i przebiegu pojazdu. Dodatkowo aplikacja będzie umożliwiać analizę parametrów pracy silnika, takich jak temperatura czy obroty, na podstawie danych diagnostycznych OBD-II, w celu monitorowania stanu technicznego samochodu i identyfikowania potencjalnych nieprawidłowości.

## Skład zespołu XIX
- Piotr Andres
- Paweł Podedworny

## Technologia
Kotlin + Jetpack Compose

## Aktualny stan aplikacji

Działają: garaż i dodawanie aut, ekran pojazdu, usuwanie z dwoma potwierdzeniami, historia serwisowa z kosztami oraz przypomnienia według daty i przebiegu. Przebieg można aktualizować z ekranu samochodu. Dane są zapisywane lokalnie.

Przypomnienia pokazują status w aplikacji i pozwalają oznaczyć zadanie jako wykonane lub je przywrócić. Powiadomienia systemowe, edycja wpisów, pozaserwisowe koszty eksploatacji i OBD-II pozostają do wdrożenia. Poniższa tabela opisuje zakres docelowy projektu; częściowo zrealizowane funkcje nadal wymagają rozwoju.

## Funkcjonalności

| Priorytet | Funkcjonalność | Status |
|---|---|---|
| Must have | Dodawanie i edycja danych samochodu | ✅ Zrobione |
| Must have | Prowadzenie historii napraw i czynności serwisowych | ✅ Zrobione |
| Must have | Zapisywanie kosztów eksploatacji | Częściowo (koszty serwisowe działają, brak pozaserwisowych) |
| Must have | Przypomnienia o przeglądach i wymianach na podstawie daty lub przebiegu | ✅ Zrobione |
| Must have | Prezentacja zapisanych danych w czytelnej formie | ✅ Zrobione |
| Should have | Analiza parametrów pracy silnika na podstawie danych OBD-II | Do zrobienia |
| Should have | Wykresy zmian parametrów w czasie | Do zrobienia |
| Could have | Wykrywanie potencjalnych nieprawidłowości na podstawie odczytów | Do zrobienia |

## Postęp prac
- 08.11 — Garaż, dodawanie aut, ekran pojazdu z 4 zakładkami, historia serwisowa, koszty, przypomnienia, usuwanie z potwierdzeniem, aktualizacja przebiegu, testy jednostkowe
- 11.11 — Edycja danych samochodu z poziomu ekranu pojazdu
