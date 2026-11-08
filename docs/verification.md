# Weryfikacja aplikacji — 2026-11-08

## Wynik

Przejrzano warstwy UI, ViewModeli, walidacji i lokalnego zapisu. Końcowe sprawdzenie zakończyło się powodzeniem:

- 40 testów jednostkowych, 0 błędów.
- 11 testów Androida, 0 błędów; emulator Pixel 9, Android 17 / API 37.
- Android Lint: 0 błędów, 24 ostrzeżenia.
- `git diff --check`: bez błędów formatowania patcha.

Polecenie: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --offline --no-configuration-cache`.

## Znalezione i naprawione

1. Ponawianie niejednoznacznego zapisu auta mogło utworzyć duplikat, bo przy każdej próbie powstawał nowy identyfikator. Nowy test odtworzył błąd przed poprawką. Identyfikator szkicu jest teraz zachowywany w SavedStateHandle, a repozytorium zapisuje samochód po ID bez duplikowania. Osobny test urządzeniowy potwierdza zachowanie istniejącej historii przy ponowionym zapisie auta.
2. Wielokrotne ponowienie odczytu mogło pozostawiać równoległe żądania, a starszy wynik mógł zastąpić nowszy. Każdy ViewModel odczytujący kolekcję anuluje poprzednie żądanie. Test regresji sprawdza odwróconą kolejność odpowiedzi.
3. Dialog aktualizacji przebiegu nie miał przewijania zawartości. Dodano przewijanie, aby formularz mieścił się przy ograniczonej wysokości okna.
4. Dodano jednoznaczny opis dostępności przycisku kalendarza przypomnień.

## Potwierdzone scenariusze

- Walidacja danych auta oraz granic rocznika i przebiegu.
- Wybór starszych roczników (1995, 2004) w faktycznym interfejsie.
- Nawigacja do pojazdu i powrót po usunięciu; oba etapy potwierdzenia, anulowanie i błąd zapisu (testy ViewModelu).
- Wpis serwisowy: formularz → lokalny zapis → historia → podsumowanie kosztów w UI; rzeczywiste wpisywanie kwoty z klawiaturą.
- Dokładność kwot, odróżnienie kosztu nieznanego od zera, daty i wpisy historyczne.
- Przypomnienie według daty: kalendarz, zachowanie szkicu po zamknięciu i ponownym otwarciu, zapis.
- Przypomnienie kilometrowe: zapis → zmiana przebiegu → zmiana statusu → oznaczenie wykonania w UI.
- Przywracanie zadania, progi dat i kilometrów, błędy zapisu i ponawianie.
- Odczyt danych przez nową instancję repozytorium, oddzielenie danych pojazdów, brak duplikatów przy ponowieniu.
- Usunięcie auta wraz z historią i przypomnieniami, odrzucenie zapisu do nieistniejącego auta.
- Odrzucenie zmniejszenia przebiegu również na poziomie repozytorium.

## Pozostałe ograniczenia

Ostrzeżenia Lint dotyczą dostępnych aktualizacji zależności, nieużywanych zasobów szablonu, powielonej etykiety aktywności i sugestii użycia KTX dla SharedPreferences. W repozytoriach jawne `commit()` służy do sprawdzania wyniku zapisu; zamiana na domyślne asynchroniczne `edit` zmieniłaby tę semantykę.

Testy UI uruchomiono na jednym emulatorze. Nie potwierdzają zgodności na wszystkich wspieranych wersjach Androida, wszystkich rozmiarach ekranu ani pełnego odtworzenia procesu przez system. Testy SavedStateHandle sprawdzają odtwarzanie danych formularzy, ale nie zastępują testu zabicia procesu.

Dane pozostają w lokalnym magazynie JSON/SharedPreferences. Nie wdrożono powiadomień systemowych, edycji wpisów i przypomnień, kosztów pozaserwisowych ani OBD-II. Przypomnienia są prezentowane wewnątrz aplikacji zgodnie z opisem UI.
