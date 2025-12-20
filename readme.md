# 📚 Compose UI Challenge

This repository contains an Android development challenge focused on **Jetpack Compose UI**.  
The project is already set up with all the necessary dependencies, so you can focus on implementing the UI and logic according to the provided specifications.

---

## ℹ️ Information

- All the necessary libraries are already set up:
    - `hilt`
    - `kotlinx.serialization`
    - `navigation-compose`
    - `coil`
- The project uses **Android Gradle Plugin (AGP)** version:  `8.13.0`
- Make sure you have the **latest version of Android Studio** installed.

---

## 🧩 Requirements

- For UI development, **Jetpack Compose** must be used.
- For navigation, the **Compose Navigation** library should be used.
- For dependency injection, the **Hilt** library should be used.
- You can use the **Coil** image loading library for loading images from URLs.
- Files containing the comment `DO NOT MODIFY` must **not** be edited.

---

## 📊 Evaluation Criteria

When reviewing your solution, we will focus on the following aspects:

- **Architecture** – 45%  
  How well the project is structured, including separation of concerns, scalability, and overall code organization.

- **UI Implementation** – 30%  
  How closely the UI matches the provided designs and how well Jetpack Compose is used.

- **Search Functionality** – 15%  
  How effectively the search feature is implemented.

- **Code Style & Clean Code** – 10%  
  Code readability, naming conventions, and adherence to clean code practices.

---

## 🧠 Task details

This challenge requires you to create an app about **Books**.  
Please find screenshots of the UIs that should be implemented in the [screenshots](screenshots)

Your implementation should include the following:

1. Implement a **loading state** for the **Search Books** screen.
2. After loading, display a **list of books** along with a **search field**.
3. The height of the list item should be **120.dp**.
4. The image in the Book item should have an aspect ratio of **3:4**.
5. Use `TextField` for search input
6. Implement **search by name** functionality. The loading indicator shouldn't be visible during a search.
7. A simple animation should be applied to list items when they appear or disappear as a result of the search functionality.
8. When the **Details** button is clicked, navigate to the **Book Details** screen and pass the `bookId` as an argument.
9. Add a **loading state** for the **Book Details** screen.
10. The image in the **Book Details** screen should have an aspect ratio of **1:1**.
11. Scrollable content should be drawn under the system bars.
12. Use the **already created custom Typography** for all text styles instead of defining new ones.
13. **Landscape support** is not required for this challenge.

**Bonus task**: Implement a shared element transition for the book image when navigating from the list to the details screen.

## 🛠️ Development Guidelines

### Do's

-   Write clean, maintainable code and follow the best practices and coding standards.
-   You are free to use any official documentation or language references (Kotlin, Android, etc).
-   You can use the debugging tools and native IDE features (only standard Auto-Completion)

### Don'ts

-   DO NOT use any external libraries for the implementation.
-   DO NOT use any Coding Assistants like GitHub Copilot, ChatGPT, etc or any other AI based tools.
-   DO NOT visit direct blogs or articles related to implementation of the tasks.
-   DO NOT use Stackoverflow or any other forum websites.
