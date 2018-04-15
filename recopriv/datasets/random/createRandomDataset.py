from random import randint, sample

N = 900 # Number of users
M = 1600 # Number of items
MAXRATING = 5 # Max int rating

ITEMS = [i for i in range(1, M+1)]

f = open('ratings.csv', 'w')

for user in range(1, N+1):
	profileSize = randint(0, M)
	profileItems = sample(ITEMS, profileSize)
	for item in profileItems:
		rating = randint(1,MAXRATING)
		f.write( str(user) + ',' + str(item) + ',' + str(rating) + ',0\n')
f.close()
