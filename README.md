# E-Commerce POC (Spring Boot + Angular)
 
An e-commerce application is a software platform that allows businesses and individuals to buy and sell goods or services over the internet. It serves as the digital infrastructure for managing online retail operations, encompassing everything from displaying products to processing payments and managing inventory.

It is more than just a website; it is an integrated system designed to handle the entire lifecycle of an online transaction. This architecture can range from simple setups to complex, scalable solutions, such as a full-stack Proof of Concept (POC) platform built using Angular, Spring Boot, MySQL, Kafka, and ElasticSearch. **Angular**, **Spring Boot**, **MySQL**, **Kafka**, and **ElasticSearch**.
 
## Features
 
- **Dynamic Product Catalog & Coupons** — Angular front-end  

- **Shopping Cart & Checkout Flow** — Fully interactive frontend with Angular 

- **Scalable Backend API** — Built on Spring Boot + MySQL  

- **Asynchronous Operations** —Powered by Kafka (e.g., Bulk products uploading)  

- **Fast & Flexible Search** — Integrated ElasticSearch for fast product discovery  

- **End-to-End Development Setup** Complete runnable environment for testing & scaling  
 
---
 
#  Project Setup Guide
 
Follow the steps below to run the application locally.
 
---
 
##  Prerequisites
 
Make sure the following tools and services are installed and running before setting up the project:
 
- **Node.js & npm** — LTS version 18 or higher  

- **Angular CLI**  

  ```bash

  npm install -g @angular/cli

- **Java Development Kit (JDK)**: Version 17 or higher

- **Maven:** Required for building and running the backend

- **Infrastructure Services:** Running instances of **MySQL, Kafka,** and **ElasticSearch**.

## Database Schema

Tables:

- User

- address

- customer

- profile

- customer_order

- cart

- otp

- cart_items

- discounts

- order_item

- reviews

- payments

- coupons

 ## Installation

  1.Clone the repository


``` bash

 https://github.com/Nagannagariakhila/Ecomm_app.git
  
2.Open MySQL and create a database
 CREATE DATABASE ecomm;
