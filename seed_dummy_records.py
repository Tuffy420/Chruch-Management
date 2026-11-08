import asyncio
import asyncpg
import datetime
import random

DB_URL = "postgresql://graceconnect_postgres_user:fjbn7KVyMaWF7Q4byydAnkDKGVdlYIbF@dpg-dbo6u6hsrm7s73b4phi0-a.oregon-postgres.render.com:5432/graceconnect_postgres"

async def seed():
    conn = await asyncpg.connect(DB_URL)
    print("Connected to database successfully.")

    today = datetime.date.today() # e.g. 2026-11-08 or today's date
    print(f"Current Date for reference: {today}")

    async with conn.transaction():
        # Clear existing test data if any, keeping users
        await conn.execute("DELETE FROM accounts;")
        await conn.execute("DELETE FROM members;")
        await conn.execute("DELETE FROM families;")
        await conn.execute("DELETE FROM events;")
        print("Cleared existing test records.")

        # 1. Insert 15 Families
        family_data = [
            ("The Johnson Family", "12 Bethesda Gardens, Grace Avenue", "+919840123456", "johnson.family@graceconnect.org", today - datetime.timedelta(days=360)),
            ("The Thomas Family", "45 Mount Zion Street, St. Thomas Town", "+919840234567", "thomas.family@graceconnect.org", today - datetime.timedelta(days=300)),
            ("The David Family", "88 Living Waters Enclave, Olive Hill", "+919840345678", "david.family@graceconnect.org", today - datetime.timedelta(days=250)),
            ("The Mathew Family", "24 Jordan Valley Road, Bethel Park", "+919840456789", "mathew.family@graceconnect.org", today - datetime.timedelta(days=200)),
            ("The Abraham Family", "7 Carmel Heights, New Hope Nagar", "+919840567890", "abraham.family@graceconnect.org", today - datetime.timedelta(days=180)),
            ("The Daniel Family", "19 Sharon Rose Colony, Canaan Street", "+919840678901", "daniel.family@graceconnect.org", today - datetime.timedelta(days=150)),
            ("The Samuel Family", "102 Ebenezer Way, Faith Cross", "+919840789012", "samuel.family@graceconnect.org", today - datetime.timedelta(days=120)),
            ("The Paul Family", "31 Galilee Shores, Peace Haven", "+919840890123", "paul.family@graceconnect.org", today - datetime.timedelta(days=100)),
            ("The Wilson Family", "55 Mount Horeb Lane, Zion City", "+919840901234", "wilson.family@graceconnect.org", today - datetime.timedelta(days=90)),
            ("The George Family", "14 Living Word Vista, Siloam Layout", "+919841012345", "george.family@graceconnect.org", today - datetime.timedelta(days=80)),
            ("The Joseph Family", "73 Trinity Crest, Grace Meadows", "+919841123456", "joseph.family@graceconnect.org", today - datetime.timedelta(days=60)),
            ("The Joshua Family", "6 Jericho Wall Road, Victory Colony", "+919841234567", "joshua.family@graceconnect.org", today - datetime.timedelta(days=50)),
            ("The Peter Family", "89 Fisherman Court, Galilee Bay", "+919841345678", "peter.family@graceconnect.org", today - datetime.timedelta(days=40)),
            ("The Philip Family", "16 Emmaus Walk, Disciples Row", "+919841456789", "philip.family@graceconnect.org", today - datetime.timedelta(days=30)),
            ("The Timothy Family", "42 Heritage Pines, Faithful Way", "+919841567890", "timothy.family@graceconnect.org", today - datetime.timedelta(days=15))
        ]

        family_ids = []
        for name, addr, ph, em, dt in family_data:
            fid = await conn.fetchval(
                "INSERT INTO families (name, address, phone, email, date_registered) VALUES ($1, $2, $3, $4, $5) RETURNING id;",
                name, addr, ph, em, dt
            )
            family_ids.append(fid)
        print(f"Inserted {len(family_ids)} families.")

        # 2. Insert 30 Members
        # Note: Ensure today (month={today.month}, day={today.day}) and nearby days have birthdays & wedding anniversaries!
        members_data = [
            # Birthdays TODAY (for testing immediate WhatsApp wishes)
            ("David", "Johnson", "Male", datetime.date(1988, today.month, today.day), "Baptized", "Married", datetime.date(2014, 5, 20), "+919840123456", "david.j@gmail.com", "Senior Software Architect", family_ids[0]),
            ("Sarah", "Thomas", "Female", datetime.date(1994, today.month, today.day), "Baptized", "Married", datetime.date(2018, today.month, today.day), "+919840234567", "sarah.t@gmail.com", "High School Teacher", family_ids[1]),
            
            # Anniversaries TODAY (both birthday and anniversary)
            ("Samuel", "Thomas", "Male", datetime.date(1991, 4, 15), "Baptized", "Married", datetime.date(2018, today.month, today.day), "+919840234568", "samuel.t@gmail.com", "Mechanical Engineer", family_ids[1]),
            
            # Birthdays in next 2-5 days
            ("Rebecca", "Johnson", "Female", datetime.date(1990, today.month, (today.day % 27) + 1), "Baptized", "Married", datetime.date(2014, 5, 20), "+919840123457", "rebecca.j@gmail.com", "Clinical Nutritionist", family_ids[0]),
            ("Joshua", "David", "Male", datetime.date(1995, today.month, (today.day % 27) + 2), "Baptized", "Single", None, "+919840345678", "joshua.d@gmail.com", "Graphic Designer", family_ids[2]),
            ("Hannah", "Mathew", "Female", datetime.date(2001, today.month, (today.day % 27) + 3), "Baptized", "Single", None, "+919840456789", "hannah.m@gmail.com", "Medical Student", family_ids[3]),
            
            # Anniversaries in next 2-5 days
            ("Abraham", "Lincoln", "Male", datetime.date(1978, 6, 12), "Baptized", "Married", datetime.date(2005, today.month, (today.day % 27) + 1), "+919840567890", "abraham.l@gmail.com", "Chartered Accountant", family_ids[4]),
            ("Rachel", "Abraham", "Female", datetime.date(1982, 8, 24), "Baptized", "Married", datetime.date(2005, today.month, (today.day % 27) + 1), "+919840567891", "rachel.a@gmail.com", "Professor", family_ids[4]),

            # General diverse congregation members
            ("Daniel", "Raj", "Male", datetime.date(1985, 3, 14), "Baptized", "Married", datetime.date(2012, 10, 18), "+919840678901", "daniel.r@gmail.com", "Civil Engineer", family_ids[5]),
            ("Esther", "Daniel", "Female", datetime.date(1989, 9, 28), "Baptized", "Married", datetime.date(2012, 10, 18), "+919840678902", "esther.d@gmail.com", "Interior Decorator", family_ids[5]),
            ("Nathan", "Daniel", "Male", datetime.date(2015, 12, 5), "Not Baptized", "Single", None, "+919840678903", "nathan.d@gmail.com", "Student", family_ids[5]),
            
            ("Jonathan", "Samuel", "Male", datetime.date(1975, 1, 20), "Baptized", "Married", datetime.date(2002, 7, 11), "+919840789012", "jonathan.s@gmail.com", "Bank Manager", family_ids[6]),
            ("Grace", "Samuel", "Female", datetime.date(1979, 11, 25), "Baptized", "Married", datetime.date(2002, 7, 11), "+919840789013", "grace.s@gmail.com", "HR Director", family_ids[6]),
            
            ("Paul", "Sundar", "Male", datetime.date(1983, 5, 8), "Baptized", "Married", datetime.date(2011, 2, 14), "+919840890123", "paul.s@gmail.com", "Marketing Lead", family_ids[7]),
            ("Mercy", "Paul", "Female", datetime.date(1986, 7, 19), "Baptized", "Married", datetime.date(2011, 2, 14), "+919840890124", "mercy.p@gmail.com", "Music Teacher", family_ids[7]),
            ("Aaron", "Paul", "Male", datetime.date(2013, 8, 30), "Baptized", "Single", None, "+919840890125", "aaron.p@gmail.com", "Student", family_ids[7]),

            ("Wilson", "Fernandez", "Male", datetime.date(1968, 2, 17), "Baptized", "Married", datetime.date(1996, 6, 22), "+919840901234", "wilson.f@gmail.com", "Retired Govt Officer", family_ids[8]),
            ("Elizabeth", "Wilson", "Female", datetime.date(1972, 4, 3), "Baptized", "Married", datetime.date(1996, 6, 22), "+919840901235", "elizabeth.w@gmail.com", "Homemaker", family_ids[8]),

            ("George", "Alexander", "Male", datetime.date(1992, 10, 11), "Baptized", "Single", None, "+919841012345", "george.a@gmail.com", "Data Analyst", family_ids[9]),
            ("Maria", "Alexander", "Female", datetime.date(1996, 3, 29), "Baptized", "Single", None, "+919841012346", "maria.a@gmail.com", "Nurse", family_ids[9]),

            ("Joseph", "Kurian", "Male", datetime.date(1980, 11, 15), "Baptized", "Married", datetime.date(2008, 9, 8), "+919841123456", "joseph.k@gmail.com", "Business Owner", family_ids[10]),
            ("Mary", "Joseph", "Female", datetime.date(1984, 1, 5), "Baptized", "Married", datetime.date(2008, 9, 8), "+919841123457", "mary.j@gmail.com", "Pharmacist", family_ids[10]),

            ("Caleb", "Joshua", "Male", datetime.date(1998, 8, 14), "Baptized", "Single", None, "+919841234567", "caleb.j@gmail.com", "UX Designer", family_ids[11]),
            
            ("Simon", "Peter", "Male", datetime.date(1977, 4, 25), "Baptized", "Married", datetime.date(2004, 12, 18), "+919841345678", "simon.p@gmail.com", "Contractor", family_ids[12]),
            ("Martha", "Peter", "Female", datetime.date(1981, 6, 30), "Baptized", "Married", datetime.date(2004, 12, 18), "+919841345679", "martha.p@gmail.com", "Event Planner", family_ids[12]),

            ("Philip", "Matthews", "Male", datetime.date(1987, 12, 22), "Baptized", "Married", datetime.date(2016, 4, 28), "+919841456789", "philip.m@gmail.com", "Sales Manager", family_ids[13]),
            ("Lydia", "Philip", "Female", datetime.date(1991, 5, 17), "Baptized", "Married", datetime.date(2016, 4, 28), "+919841456790", "lydia.p@gmail.com", "Fashion Designer", family_ids[13]),

            ("Timothy", "Varghese", "Male", datetime.date(1999, 10, 4), "Baptized", "Single", None, "+919841567890", "timothy.v@gmail.com", "AI Researcher", family_ids[14]),
            ("Eunice", "Varghese", "Female", datetime.date(1970, 7, 21), "Baptized", "Widowed", None, "+919841567891", "eunice.v@gmail.com", "Retired Educator", family_ids[14]),
            ("Priscilla", "Devi", "Female", datetime.date(1993, 2, 11), "Baptized", "Single", None, "+919841567892", "priscilla.d@gmail.com", "Content Creator", family_ids[14])
        ]

        member_ids = []
        for fn, ln, gen, dob, b_stat, m_stat, m_date, mob, em, occ, fam_id in members_data:
            avatar = f"https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" if gen == "Female" else "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
            mid = await conn.fetchval("""
                INSERT INTO members (first_name, last_name, gender, dob, baptism_status, marriage_status, 
                                     marriage_date, mobile_number, email, occupation, profile_photo, family_id)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12) RETURNING id;
            """, fn, ln, gen, dob, b_stat, m_stat, m_date, mob, em, occ, avatar, fam_id)
            member_ids.append(mid)
        print(f"Inserted {len(member_ids)} church members.")

        # 3. Insert 50 Accounts Records (28 Income: Sunday Offerings, Tithes, Thanksgiving + 22 Expense: Church expenses)
        income_funds = [
            "Sunday Offering", 
            "Tithes & Firstfruits", 
            "Thanksgiving Offering", 
            "Mission & Outreach Fund", 
            "Building & Renovation Fund", 
            "Youth Ministry Offering"
        ]
        
        expense_funds = [
            "Church Electricity & Power Bill",
            "Sound System & Live Stream Maintenance",
            "Church Cleaning & Sanitization Services",
            "Guest Speaker Honorarium & Travel",
            "Sunday School & VBS Materials",
            "Charity & Widow Support Welfare",
            "Communion Elements & Altar Supplies",
            "Church Van Fuel & Transportation",
            "Water, Sanitation & Facility Upkeep",
            "Musical Instrument Repairs & Tuning"
        ]

        accounts_records = []
        
        # 28 Income Records
        sample_members = ["David Johnson", "Sarah Thomas", "Samuel Thomas", "Abraham Lincoln", "Daniel Raj", 
                          "Jonathan Samuel", "Paul Sundar", "Wilson Fernandez", "Joseph Kurian", "Philip Matthews",
                          "General Congregation", "Anonymous Blessing", "Youth Group Fellowship"]
        
        for i in range(28):
            dt = today - datetime.timedelta(days=(i * 2) % 60)
            fund = random.choice(income_funds)
            if "Sunday" in fund:
                # Align date roughly to Sundays
                amount = random.choice([15000.0, 18500.0, 22400.0, 27600.0, 31200.0, 35000.0, 42000.0])
                mem = "General Sunday Congregation"
                notes = f"Regular Sunday Service bag collection and digital kiosk offerings"
            elif "Tithe" in fund:
                amount = random.choice([5000.0, 8000.0, 12000.0, 15000.0, 20000.0, 25000.0])
                mem = random.choice(sample_members[:8])
                notes = f"Monthly tithe and dedicated firstfruits contribution from {mem}"
            elif "Thanksgiving" in fund:
                amount = random.choice([2500.0, 5000.0, 7500.0, 10000.0])
                mem = random.choice(sample_members)
                notes = f"Special thanksgiving offering for prayer answered & blessing"
            else:
                amount = random.choice([3000.0, 4500.0, 6000.0, 10000.0, 15000.0])
                mem = random.choice(sample_members)
                notes = f"Designated contribution for {fund}"
            
            accounts_records.append((amount, dt, fund, mem, notes, "INCOME"))

        # 22 Expense Records
        for j in range(22):
            dt = today - datetime.timedelta(days=(j * 3) % 65)
            fund = random.choice(expense_funds)
            if "Electricity" in fund:
                amount = random.choice([6500.0, 8200.0, 9400.0, 11500.0])
                vendor = "State Electricity Board"
                notes = "Main sanctuary, fellowship hall & nursery AC electricity bill payment"
            elif "Sound" in fund:
                amount = random.choice([3500.0, 5200.0, 7800.0])
                vendor = "Apex Audio Engineering"
                notes = "Wireless mic replacement, digital mixer checkup and cabling repair"
            elif "Cleaning" in fund:
                amount = random.choice([2500.0, 3000.0, 3500.0])
                vendor = "Sparkle Cleaners Ltd"
                notes = "Weekly deep cleaning, sanctuary carpet vacuuming and sanitization"
            elif "Speaker" in fund:
                amount = random.choice([5000.0, 7500.0, 10000.0])
                vendor = "Guest Evangelist"
                notes = "Honorarium & travel reimbursement for guest visiting preacher"
            elif "Charity" in fund:
                amount = random.choice([4000.0, 6000.0, 8500.0])
                vendor = "Community Food Pantry"
                notes = "Groceries & medical aid kit distribution for underprivileged families"
            elif "Communion" in fund:
                amount = random.choice([1200.0, 1800.0, 2400.0])
                vendor = "Bethlehem Christian Supplies"
                notes = "Unleavened bread & pure communion juice cups for First Sunday Holy Communion"
            else:
                amount = random.choice([1500.0, 2200.0, 3800.0, 4500.0])
                vendor = "Facility Maintenance"
                notes = f"Operational maintenance expense: {fund}"

            accounts_records.append((amount, dt, fund, vendor, notes, "EXPENSE"))

        # Insert all 50 accounts records
        for amt, dt, fnd, m_name, nts, typ in accounts_records:
            await conn.execute("""
                INSERT INTO accounts (amount, date, fund, member_name, notes, type)
                VALUES ($1, $2, $3, $4, $5, $6);
            """, amt, dt, fnd, m_name, nts, typ)
        print(f"Inserted {len(accounts_records)} financial accounts records (Offerings & Expenses).")

        # 4. Insert 8 Events
        events_data = [
            ("Sunday Miracle & Worship Service", today + datetime.timedelta(days=(6 - today.weekday()) % 7), "09:00 AM - 11:30 AM", "Main Worship Sanctuary", "Worship"),
            ("Midweek Power Night & Intercession", today + datetime.timedelta(days=(2 - today.weekday()) % 7), "07:00 PM - 08:30 PM", "Bethel Prayer Hall", "Prayer"),
            ("Ignite Youth Fellowship & Jam Session", today + datetime.timedelta(days=(5 - today.weekday()) % 7), "05:30 PM - 07:30 PM", "Youth Activity Lounge", "Youth"),
            ("Joyful Melodies Choir Rehearsal", today + datetime.timedelta(days=(4 - today.weekday()) % 7), "06:30 PM - 08:00 PM", "Choir Balcony", "Choir"),
            ("Grace Outreach Community Medical Camp", today + datetime.timedelta(days=14), "08:30 AM - 02:00 PM", "Community Community Center", "Outreach"),
            ("Holy Communion & Thanksgiving Service", today + datetime.timedelta(days=21), "09:00 AM - 12:00 PM", "Main Worship Sanctuary", "Worship"),
            ("Annual Couples & Family Life Retreat", today + datetime.timedelta(days=30), "10:00 AM - 04:00 PM", "Mount Carmel Convention Center", "Special"),
            ("Sunday School Teachers Planning Meet", today + datetime.timedelta(days=7), "11:45 AM - 01:00 PM", "Conference Room A", "Meeting")
        ]

        for title, dt, tm, loc, cat in events_data:
            await conn.execute("""
                INSERT INTO events (title, date, time, location, category)
                VALUES ($1, $2, $3, $4, $5);
            """, title, dt, tm, loc, cat)
        print(f"Inserted {len(events_data)} church events.")

    # Verification count
    fam_count = await conn.fetchval("SELECT count(*) FROM families;")
    mem_count = await conn.fetchval("SELECT count(*) FROM members;")
    acc_count = await conn.fetchval("SELECT count(*) FROM accounts;")
    evt_count = await conn.fetchval("SELECT count(*) FROM events;")
    inc_sum = await conn.fetchval("SELECT COALESCE(SUM(amount), 0) FROM accounts WHERE type = 'INCOME';")
    exp_sum = await conn.fetchval("SELECT COALESCE(SUM(amount), 0) FROM accounts WHERE type = 'EXPENSE';")

    print(f"\n==========================================")
    print(f"DATABASE SEEDING SUCCESSFUL!")
    print(f"Families Count   : {fam_count}")
    print(f"Members Count    : {mem_count}")
    print(f"Accounts Count   : {acc_count} (50 Dummy records: 28 Income, 22 Expense)")
    print(f"Events Count     : {evt_count}")
    print(f"Total Income     : ₹{inc_sum:,.2f}")
    print(f"Total Expense    : ₹{exp_sum:,.2f}")
    print(f"Net Church Fund  : ₹{(inc_sum - exp_sum):,.2f}")
    print(f"==========================================")

    await conn.close()

if __name__ == "__main__":
    asyncio.run(seed())

