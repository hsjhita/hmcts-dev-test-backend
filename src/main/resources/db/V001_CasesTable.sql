-- =============================================
-- Author:		Harpreet Jhita
--
-- Create date: 30-MAY-2025
-- Description:	Create cases table to store case information
-- VERSION	  :	30-MAY-2025		1.0  - Initial creation
-- =============================================

create table cases
(
  id           integer generated always as identity
    constraint cases_pk
      primary key,
  case_number  integer,
  title        varchar   not null,
  description  varchar,
  created_date timestamp default now(),
);
