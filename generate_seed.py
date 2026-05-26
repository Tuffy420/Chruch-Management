import csv
import random
from datetime import datetime, timedelta

def generate_data():
    # 1. Generate Families (30 records)
    family_names = [
        "Miller", "Brooks", "Henderson", "Patterson", "Chavez", "Sharma", "Patel", "Singh", "Kumar", "Mehta",
        "Joshi", "Iyer", "Nair", "Reddy", "Rao", "Smith", "Johnson", "Williams", "Brown", "Jones",
        "Garcia", "Davis", "Rodriguez", "Martinez", "Thomas", "Anderson", "Taylor", "Moore", "Jackson", "Martin"
    ]
    
    addresses = [
        "742 Evergreen Terrace, Springfield", "304 Maple Ave, Riverdale", "12 Blossom Way, Oakville",
        "89 Pine Road, Seattle", "556 Cedar Blvd, Miami", "100 MG Road, Mumbai", "107 Park Street, Bangalore",
        "114 Evergreen Terrace, Delhi", "121 Maple Avenue, Chennai", "128 Pine Road, Kolkata",
        "135 Cedar Blvd, Springfield", "142 Lakeview Drive, Riverdale", "149 Hillside Ave, Oakville",
        "156 Sunset Blvd, Seattle", "163 Broadway, Miami", "170 MG Road, Mumbai", "177 Park Street, Bangalore",
        "184 Evergreen Terrace, Delhi", "191 Maple Avenue, Chennai", "198 Pine Road, Kolkata",
        "205 Cedar Blvd, Springfield", "212 Lakeview Drive, Riverdale", "219 Hillside Ave, Oakville",
        "226 Sunset Blvd, Seattle", "233 Broadway, Miami", "240 MG Road, Mumbai", "247 Park Street, Bangalore",
        "254 Evergreen Terrace, Delhi", "261 Maple Avenue, Chennai", "268 Pine Road, Kolkata"
    ]
    
    families = []
    for i, name in enumerate(family_names):
        fam_id = i + 1
        fam_email = f"{name.lower()}.fam@graceconnect.org"
        fam_phone = f"+91 98765 {10000 + i}"
        reg_date = (datetime(2024, 1, 1) + timedelta(days=i*10)).strftime('%Y-%m-%d')
        
        families.append({
            "id": fam_id,
            "name": f"{name} Household",
            "address": addresses[i],
            "email": fam_email,
            "phone": fam_phone,
            "date_registered": reg_date
        })

    # 2. Generate Members (100 records mapped to families)
    genders = ["Male", "Female"]
    occupations = ["Software Engineer", "Teacher", "Physician", "Architect", "Designer", "Accountant", "Student", "Retired", "Homemaker", "Lawyer"]
    marriage_statuses = ["Married", "Single", "Widowed"]
    baptism_statuses = ["Baptized", "Unbaptized"]
    
    first_names_male = [
        "James", "Tommy", "Raymond", "Liam", "Mateo", "Rahul", "Amit", "Anil", "Vikram", "Ramesh",
        "John", "Michael", "Thomas", "Daniel", "Matthew", "Vijay", "Rajesh", "Suresh", "Deepak", "Joseph",
        "Charles", "David", "Kevin", "Brian", "Edward", "Keith", "Peter", "Walter", "Harold", "Arthur",
        "Douglas", "Lawrence", "Albert", "Joe", "Stephen", "Bobby", "Billy", "Jack", "Wayne", "Gary"
    ]
    first_names_female = [
        "Sarah", "Emily", "Charlotte", "Elena", "Ananya", "Sita", "Sunita", "Anita", "Divya", "Patricia",
        "Linda", "Barbara", "Kiran", "Geeta", "Ritu", "Jennifer", "Elizabeth", "Susan", "Jessica", "Mary",
        "Ruth", "Jane", "Grace", "Ashley", "Alice", "Helen", "Donna", "Carol", "Gloria", "Marie",
        "Ann", "Julia", "Karen", "Diana", "Pamela", "Lori", "Sandra", "Nancy", "Lisa", "Karen"
    ]
    
    members = []
    member_names_list = []
    
    for i in range(100):
        mem_id = i + 1
        gender = random.choice(genders)
        if gender == "Male":
            first_name = first_names_male[i % len(first_names_male)]
        else:
            first_name = first_names_female[i % len(first_names_female)]
            
        last_name = families[i % len(families)]["name"].split(" ")[0]
        dob = (datetime(1970, 1, 1) + timedelta(days=random.randint(0, 15000))).strftime('%Y-%m-%d')
        mobile = f"+91 98765 {20000 + i}"
        email = f"{first_name.lower()}.{last_name.lower()}@graceconnect.org"
        occupation = random.choice(occupations)
        marriage = "Married" if (i % 3 == 0) else random.choice(marriage_statuses)
        marriage_date = ""
        if marriage == "Married":
            marriage_date = (datetime(2005, 1, 1) + timedelta(days=random.randint(0, 7000))).strftime('%Y-%m-%d')
            
        baptism = random.choice(baptism_statuses)
        photo = f"https://placehold.co/150/8b5cf6/ffffff?text={first_name[0]}"
        
        # Link member to their corresponding family
        fam = families[i % len(families)]
        
        members.append({
            "id": mem_id,
            "first_name": first_name,
            "last_name": last_name,
            "gender": gender,
            "dob": dob,
            "mobile_number": mobile,
            "email": email,
            "occupation": occupation,
            "marriage_status": marriage,
            "marriage_date": marriage_date,
            "baptism_status": baptism,
            "profile_photo": photo,
            "family_id": fam["id"]
        })
        member_names_list.append(f"{first_name} {last_name}")

    # 3. Generate Events (20 records)
    event_titles = [
        ("Midweek Prayer Gathering", "Service", "Main Chapel"),
        ("Sunday Worship & Holy Communion", "Service", "Sanctuary Main Hall"),
        ("Youth Group Fellowship Night", "Fellowship", "Youth Activities Hall"),
        ("Charity Food Drive Outreach", "Charity", "Community Kitchen"),
        ("Worship Team Rehearsal", "Fellowship", "Choir Room"),
        ("Men's Morning Fellowship", "Fellowship", "Fellowship Room"),
        ("Women's Bible Study Group", "Fellowship", "Main Library"),
        ("Sunday School Teachers Prep", "Fellowship", "Classroom A"),
        ("Orphanage Support Visit", "Charity", "City Orphanage"),
        ("GraceConnect Community Seminar", "Fellowship", "Auditorium"),
        ("Senior Citizens Tea Social", "Fellowship", "Fellowship Hall"),
        ("Missions Team Planning Session", "Fellowship", "Board Room"),
        ("Friday Evening Praise Night", "Service", "Sanctuary Main Hall"),
        ("Homeless Shelter Soup Service", "Charity", "Shelter Dining Room"),
        ("Young Adults Fellowship Dinner", "Fellowship", "Fellowship Hall"),
        ("Vacation Bible School VBS", "Service", "Entire Church Campus"),
        ("Sunday Evening Vespers Service", "Service", "Main Chapel"),
        ("Couples Ministry Retreat Info Session", "Fellowship", "Classroom B"),
        ("Neighborhood Clean Up Outreach", "Charity", "Springfield Community Park"),
        ("Monthly Deacon & Leadership Meeting", "Fellowship", "Board Room")
    ]
    
    events = []
    for i, (title, cat, loc) in enumerate(event_titles):
        event_id = i + 1
        date = (datetime(2026, 5, 26) + timedelta(days=i - 5)).strftime('%Y-%m-%d')
        time_str = f"{random.randint(9, 19):02d}:00:00"
        
        events.append({
            "id": event_id,
            "title": title,
            "date": date,
            "time": time_str,
            "location": loc,
            "category": cat
        })

    # 4. Generate Accounts (50 records)
    funds = [
        "Sunday Worship Offering",
        "General Fund",
        "Missions & Outreach",
        "Building Maintenance",
        "Charity & Welfare"
    ]
    
    accounts = []
    for i in range(50):
        t_id = i + 1
        t_type = "INCOME" if random.random() > 0.3 else "EXPENSE"
        amount = round(random.uniform(500, 15000), 2)
        date = (datetime(2026, 5, 26) - timedelta(days=random.randint(0, 60))).strftime('%Y-%m-%d')
        fund = random.choice(funds)
        
        if t_type == "INCOME":
            member_name = random.choice(member_names_list)
            notes = f"Monthly contribution to {fund}"
        else:
            member_name = "General Vendor"
            notes = f"Payment for {fund} operational bills"
            
        accounts.append({
            "id": t_id,
            "type": t_type,
            "member_name": member_name,
            "amount": amount,
            "fund": fund,
            "notes": notes,
            "date": date
        })

    return families, members, events, accounts

def write_csv(filename, data, fieldnames):
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(data)

def generate_sql(families, members, events, accounts, sql_filename):
    with open(sql_filename, 'w', encoding='utf-8') as f:
        f.write("-- GraceConnect Church Database Seed Script\n")
        f.write("-- Automatically generated for MySQL with Sequential Numeric IDs\n\n")
        f.write("USE graceconnect;\n\n")
        
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n")
        f.write("TRUNCATE TABLE accounts;\n")
        f.write("TRUNCATE TABLE members;\n")
        f.write("TRUNCATE TABLE families;\n")
        f.write("TRUNCATE TABLE events;\n")
        f.write("TRUNCATE TABLE users;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 1;\n\n")
        
        # Insert Users
        f.write("-- 0. Seed Users Table --\n")
        f.write("INSERT INTO users (id, email, full_name, password, role) VALUES (1, 'admin@graceconnect.org', 'System Administrator', '$2a$10$SRCwTOSwYrVhdzl.bRGhsuD6C0tI94xAmybSkRZlD/a5wHBI2CTCC', 'ADMIN');\n\n")
        
        # Insert Families
        f.write("-- 1. Seed Families Table --\n")
        for fam in families:
            f.write(f"INSERT INTO families (id, address, date_registered, email, name, phone) VALUES ({fam['id']}, '{fam['address']}', '{fam['date_registered']}', '{fam['email']}', '{fam['name']}', '{fam['phone']}');\n")
        f.write("\n")
        
        # Insert Members
        f.write("-- 2. Seed Members Table --\n")
        for m in members:
            # Escape single quotes in names/occupations
            first_name = m['first_name'].replace("'", "''")
            last_name = m['last_name'].replace("'", "''")
            occupation = m['occupation'].replace("'", "''")
            m_date_val = f"'{m['marriage_date']}'" if m['marriage_date'] else "NULL"
            f.write(f"INSERT INTO members (id, baptism_status, dob, email, first_name, gender, last_name, marriage_status, marriage_date, mobile_number, occupation, profile_photo, family_id) VALUES ({m['id']}, '{m['baptism_status']}', '{m['dob']}', '{m['email']}', '{first_name}', '{m['gender']}', '{last_name}', '{m['marriage_status']}', {m_date_val}, '{m['mobile_number']}', '{occupation}', '{m['profile_photo']}', {m['family_id']});\n")
        f.write("\n")
        
        # Insert Events
        f.write("-- 3. Seed Events Table --\n")
        for ev in events:
            title = ev['title'].replace("'", "''")
            loc = ev['location'].replace("'", "''")
            f.write(f"INSERT INTO events (id, category, date, location, time, title) VALUES ({ev['id']}, '{ev['category']}', '{ev['date']}', '{loc}', '{ev['time']}', '{title}');\n")
        f.write("\n")
        
        # Insert Accounts
        f.write("-- 4. Seed Accounts Table --\n")
        for acc in accounts:
            m_name = acc['member_name'].replace("'", "''")
            notes = acc['notes'].replace("'", "''")
            f.write(f"INSERT INTO accounts (id, amount, date, fund, member_name, notes, type) VALUES ({acc['id']}, {acc['amount']}, '{acc['date']}', '{acc['fund']}', '{m_name}', '{notes}', '{acc['type']}');\n")
        f.write("\n")
        
        print(f"SQL file written: {sql_filename}")

if __name__ == "__main__":
    families, members, events, accounts = generate_data()
    
    # Write CSVs
    write_csv("mysql_families.csv", families, ["id", "address", "date_registered", "email", "name", "phone"])
    write_csv("mysql_members.csv", members, ["id", "baptism_status", "dob", "email", "first_name", "gender", "last_name", "marriage_status", "marriage_date", "mobile_number", "occupation", "profile_photo", "family_id"])
    write_csv("mysql_events.csv", events, ["id", "category", "date", "location", "time", "title"])
    write_csv("mysql_accounts.csv", accounts, ["id", "amount", "date", "fund", "member_name", "notes", "type"])
    
    # Write SQL Script
    generate_sql(families, members, events, accounts, "graceconnect_mysql_seed.sql")
