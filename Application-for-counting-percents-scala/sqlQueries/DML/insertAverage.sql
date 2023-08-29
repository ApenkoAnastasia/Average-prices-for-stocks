USE stock_price_incrementDB;
INSERT INTO average_per_month
SELECT stockName,
       algorithmType,
       AVG(january), 
       AVG(february), 
       AVG(march), 
       AVG(april), 
       AVG(may), 
       AVG(june), 
       AVG(july), 
       AVG(august), 
       AVG(september), 
       AVG(october), 
       AVG(november), 
       AVG(december), 
       AVG(totalPerYear)
FROM increase_in_stocks
GROUP BY stockName, algorithmType;