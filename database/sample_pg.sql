create schema hr;

CREATE TABLE hr.emp (eid int generated always as identity (start with 1 increment by 1) primary key,
                  first_name varchar(40),
                  last_name varchar(40),
                  email varchar(100),
                  hire_dt timestamp
                  );

INSERT INTO hr.emp (first_name, last_name, email, hire_dt) VALUES
('Mickey', 'Mouse', 'mickey.mouse@disney.com', '1928-11-18 00:00:00'),
('Minnie', 'Mouse', 'minnie.mouse@disney.com', '1928-11-18 00:00:00'),
('Donald', 'Duck', 'donald.duck@disney.com', '1934-06-09 00:00:00'),
('Daisy', 'Duck', 'daisy.duck@disney.com', '1940-06-07 00:00:00'),
('Goofy', 'Goof', 'goofy.goof@disney.com', '1932-05-25 00:00:00'),
('Pluto', 'Dog', 'pluto.dog@disney.com', '1930-09-05 00:00:00'),
('Huey', 'Duck', 'huey.duck@disney.com', '1937-10-17 00:00:00'),
('Dewey', 'Duck', 'dewey.duck@disney.com', '1937-10-17 00:00:00'),
('Louie', 'Duck', 'louie.duck@disney.com', '1937-10-17 00:00:00'),
('Scrooge', 'McDuck', 'scrooge.mcduck@disney.com', '1947-12-22 00:00:00'),
('Bugs', 'Bunny', 'bugs.bunny@looneytunes.com', '1940-07-27 00:00:00'),
('Daffy', 'Duck', 'daffy.duck@looneytunes.com', '1937-04-17 00:00:00'),
('Porky', 'Pig', 'porky.pig@looneytunes.com', '1935-03-02 00:00:00'),
('Elmer', 'Fudd', 'elmer.fudd@looneytunes.com', '1940-03-02 00:00:00'),
('Sylvester', 'Cat', 'sylvester.cat@looneytunes.com', '1945-03-24 00:00:00'),
('Tweety', 'Bird', 'tweety.bird@looneytunes.com', '1942-11-21 00:00:00'),
('Taz', 'Devil', 'taz.devil@looneytunes.com', '1954-06-19 00:00:00'),
('Marvin', 'Martian', 'marvin.martian@looneytunes.com', '1948-07-24 00:00:00'),
('Yosemite', 'Sam', 'yosemite.sam@looneytunes.com', '1945-05-05 00:00:00'),
('Foghorn', 'Leghorn', 'foghorn.leghorn@looneytunes.com', '1946-08-31 00:00:00'),
('Speedy', 'Gonzales', 'speedy.gonzales@looneytunes.com', '1953-09-17 00:00:00'),
('Wile E.', 'Coyote', 'wile.e.coyote@looneytunes.com', '1949-09-17 00:00:00'),
('Road', 'Runner', 'road.runner@looneytunes.com', '1949-09-17 00:00:00');
