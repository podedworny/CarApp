# Architektura aplikacji

Aplikacja korzysta z jednokierunkowego przepływu danych: ekran wyświetla stan i przekazuje zdarzenia do ViewModelu, który korzysta z repozytorium.

- `MainActivity` uruchamia Compose i pobiera ViewModel.
- `CarApplication` / `AppContainer` tworzą zależności o zasięgu aplikacji (ręczne dependency injection).
- `ui/GarageApp` łączy stan z ekranami i zbiera StateFlow z uwzględnieniem cyklu życia.
- `ui/garage` zawiera ekran garażu, stan i ViewModel obsługujący bieżący przepływ garaż → dodawanie auta.
- `ui/addcar` zawiera formularz oraz jego niezmienny stan. Ekran otrzymuje dane i callbacki, bez dostępu do zapisu.
- `ui/components` zawiera współdzielone elementy interfejsu.
- `domain/model`, `domain/validation` i `domain/repository` zawierają model, reguły walidacji i kontrakt dostępu do danych, bez zależności od Androida.
- `data/LocalCarRepository` i `LocalServiceRepository` implementują lokalny zapis. Wspólny `LocalGarageStorage` serializuje dostęp do danych i wykonuje operacje I/O poza głównym wątkiem. Usunięcie samochodu usuwa jego wpisy serwisowe w tym samym zapisie; zapis wpisu sprawdza istnienie samochodu.

Formularz jest przechowywany w SavedStateHandle. Zapis trwa w viewModelScope, więc obrót ekranu go nie przerywa. Błędy zapisu pozostawiają dane formularza, a anulowanie korutyny nie jest zamieniane w błąd użytkownika. Widok pokazuje nowe auto dopiero po udanym zapisie.

Obecny adapter zachowuje format JSON w SharedPreferences, aby zachować zapisane samochody. Historia serwisowa ma obecnie osobny klucz dla każdego auta. Docelowa migracja do Room powinna przenieść samochody i wpisy, zanim wprowadzimy rozbudowane zapytania, edycję relacji i synchronizację. Ekrany korzystają z interfejsu repozytorium, więc nie wymagają zmian związanych z formatem przechowywania.

Nowe funkcje dodajemy w osobnych pakietach funkcjonalnych z własnym stanem i ViewModelem, gdy mają odrębny cykl życia. Nie dopisujemy historii serwisowej ani kosztów do GarageViewModel. Osobne przypadki użycia wprowadzamy, gdy logika domenowa jest współdzielona lub wykracza poza pojedyncze wywołanie repozytorium.

## Weryfikacja

`./gradlew :app:assembleDebug :app:testDebugUnitTest`

Testy walidacji obejmują granice rocznika i przebiegu. Testy ViewModelu korzystają z repozytorium w pamięci oraz testowego dispatchera i sprawdzają walidację przed zapisem, podwójne kliknięcie, błąd zapisu i ponowienie, błąd odczytu oraz odtwarzanie formularza.

## Historia serwisowa i koszty

`ui/service` ma osobny `VehicleServiceViewModel`, niemutowalny stan i formularz. `VehicleRoute` tworzy ViewModel z kluczem konkretnego auta i zachowuje stan wybranej zakładki przy przejściu do formularza. Szkic formularza jest zachowany w SavedStateHandle również po wyjściu z formularza; po udanym zapisie jest czyszczony.

Wpis zawiera typ, nazwę, datę, przebieg, opcjonalny koszt i notatkę. Data jest zapisana jako północ UTC dla daty kalendarzowej, zgodnie z Material DatePicker. Dzisiejsza data pochodzi z lokalnej strefy czasowej użytkownika. Wpisy historyczne mogą mieć przebieg mniejszy niż przebieg auta; nie zmieniają automatycznie aktualnego licznika.

Kwoty są przechowywane jako liczba groszy (Long). Koszt nieznany (null) nie jest tożsamy z bezpłatną czynnością (0). Podsumowanie kosztów obejmuje obecnie tylko wpisy serwisowe i wyraźnie wskazuje liczbę wpisów bez kwoty. Identyfikator wpisu jest stabilny podczas ponawiania zapisu, aby nie powstały duplikaty.

Testy jednostkowe sprawdzają walidację dat i pieniędzy, sortowanie, szkic formularza oraz błędy i ponawianie zapisu. Testy na urządzeniu używają osobnych magazynów preferencji: sprawdzają ponowny odczyt danych, oddzielenie aut, usuwanie historii z pojazdem i pełny przepływ UI od formularza do kosztów.

`./gradlew :app:connectedDebugAndroidTest`

## Przypomnienia i aktualizacja przebiegu

`ui/reminders` zawiera własny stan, formularz, listę i `ReminderViewModel`. `ReminderRepository` ma lokalny adapter korzystający z tego samego `LocalGarageStorage`, co samochody i historia serwisowa. Usunięcie pojazdu usuwa również jego przypomnienia w jednym zapisie.

Przypomnienie ma datę, docelowy przebieg lub oba progi. Obowiązuje wcześniej osiągnięty próg. Funkcja domenowa oblicza status: zaległe, do wykonania, nadchodzące (30 dni lub 1000 km), zaplanowane lub wykonane. Oznaczenie jako wykonane jest odwracalne i nie tworzy automatycznie wpisu serwisowego ani kosztu. Terminy można zapisywać również jako zaległe.

`VehicleMileageViewModel` obsługuje zmianę licznika niezależnie od formularzy. Repozytorium odrzuca zmniejszenie przebiegu. Po udanym zapisie ekran pojazdu oraz lista garażu otrzymują zapisany samochód, a przypomnienia są ponownie przeliczane. Historyczne przebiegi we wpisach serwisowych pozostają bez zmian. Formularz serwisu podpowiada teraz aktualny przebieg auta.

Datę do obliczania statusów ekran sprawdza co minutę, aby przejście przez północ zmieniało status bez ponownego uruchamiania widoku. To przypomnienia w aplikacji; nie wdrożono jeszcze planowania pracy w tle, kanałów powiadomień ani uprawnień Androida do ich wyświetlania. UI informuje o tym w formularzu i na liście.

Testy obejmują progi i walidację, błędy zapisu i powtórzenia, zachowanie szkicu, oznaczanie wykonania i przywracanie zadania. Test na emulatorze tworzy przypomnienie kilometrowe, aktualizuje przebieg, sprawdza zmianę statusu i oznacza zadanie jako wykonane. Dane testów są oddzielone od właściwego garażu.
