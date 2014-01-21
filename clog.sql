-- phpMyAdmin SQL Dump
-- version 4.1.4
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jan 21, 2014 at 05:21 PM
-- Server version: 5.5.34-MariaDB
-- PHP Version: 5.5.8

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `clog`
--

-- --------------------------------------------------------

--
-- Table structure for table `index_runs`
--

DROP TABLE IF EXISTS `index_runs`;
CREATE TABLE IF NOT EXISTS `index_runs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `datetime` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `datetime` (`datetime`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2 ;

-- --------------------------------------------------------

--
-- Table structure for table `logfileentries`
--

DROP TABLE IF EXISTS `logfileentries`;
CREATE TABLE IF NOT EXISTS `logfileentries` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `host` varchar(512) DEFAULT NULL,
  `datetime` bigint(20) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `length` int(11) DEFAULT NULL,
  `useragent` text,
  `processtime` double DEFAULT NULL,
  `forwardedfor` varchar(512) DEFAULT NULL,
  `sessiontime` double DEFAULT NULL,
  `path` text,
  `referrer` text,
  `ip` varchar(46) DEFAULT NULL,
  `inserted` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `datetime` (`datetime`),
  KEY `inserted` (`inserted`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=11918653 ;

-- --------------------------------------------------------

--
-- Table structure for table `logfiles`
--

DROP TABLE IF EXISTS `logfiles`;
CREATE TABLE IF NOT EXISTS `logfiles` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `path` varchar(512) NOT NULL,
  `length` int(11) NOT NULL DEFAULT '0',
  `lines` int(11) NOT NULL DEFAULT '0',
  `last_scan` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=132 ;

-- --------------------------------------------------------

--
-- Table structure for table `time_cache`
--

DROP TABLE IF EXISTS `time_cache`;
CREATE TABLE IF NOT EXISTS `time_cache` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `start` bigint(20) NOT NULL,
  `range` int(11) NOT NULL,
  `hits` int(11) NOT NULL,
  `processtime` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `start` (`start`,`range`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=587336 ;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
