"""Seed database with realistic data from production log analysis.

Run: python seed.py
"""
from __future__ import annotations

import asyncio
from datetime import datetime, timezone

from sqlalchemy import select, text
from app.database import engine, async_session
from app.models.base import Base
from app.models import ALL_MODELS  # noqa
from app.models import (
    Box, Datasource, Organization, User, Template, SystemConfig,
    Style, DatasourceType,
)
from passlib.context import CryptContext

pwd_ctx = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")

NOW = datetime.now(timezone.utc)


async def seed():
    # Create tables
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async with async_session() as db:
        # Check if already seeded
        r = await db.execute(select(Box).limit(1))
        if r.scalars().first():
            print("Database already seeded, skipping.")
            return

        # ── Users ──
        db.add(User(username="admin", hashedPassword=pwd_ctx.hash("admin123"),
                     fullname="管理员", auths=["AUTH_ROOT"], ct=NOW, ut=NOW))
        db.add(User(username="nurse01", hashedPassword=pwd_ctx.hash("123456"),
                     fullname="护士张三", auths=["AUTH_COMMON"], ct=NOW, ut=NOW))
        print("✓ Users created")

        # ── Organizations (from log: org=5 is the main group) ──
        orgs = [
            Organization(id=-1, name="未分组", parentid=None, idpath="-1.", ct=NOW, ut=NOW),
            Organization(id=0, name="默认分组", parentid=None, idpath="0.", ct=NOW, ut=NOW),
            Organization(id=1, name="3楼", parentid=0, idpath="0.1.", ct=NOW, ut=NOW),
            Organization(id=2, name="4楼", parentid=0, idpath="0.2.", ct=NOW, ut=NOW),
            Organization(id=3, name="5楼", parentid=0, idpath="0.3.", ct=NOW, ut=NOW),
            Organization(id=4, name="6楼", parentid=0, idpath="0.4.", ct=NOW, ut=NOW),
            Organization(id=5, name="全院", parentid=0, idpath="0.5.", ct=NOW, ut=NOW),
            Organization(id=6, name="3楼内一科", parentid=1, idpath="0.1.6.", ct=NOW, ut=NOW),
            Organization(id=7, name="3楼内二科", parentid=1, idpath="0.1.7.", ct=NOW, ut=NOW),
            Organization(id=8, name="5楼外科", parentid=3, idpath="0.3.8.", ct=NOW, ut=NOW),
            Organization(id=9, name="5楼耳鼻喉科", parentid=3, idpath="0.3.9.", ct=NOW, ut=NOW),
        ]
        db.add_all(orgs)
        print("✓ Organizations created")

        # ── Datasources (from log: leveldepart type with real department IDs) ──
        datasources = [
            Datasource(name="5楼外科大屏", type="leveldepart",
                       departmentid=[912,910,908,909,905,922,923,914,904,913,973,3333,90,92,924,930,1010,67,9006,940,939],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="5楼耳鼻喉科大屏", type="leveldepart",
                       departmentid=[916,971,972,105,941],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="6楼眼科大屏", type="leveldepart",
                       departmentid=[452,453,454,460,455],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="3楼内一科大屏", type="leveldepart",
                       departmentid=[4016,4006,4012,4009,4010,4011,4015,915,4008,9191,9192,918,917,457,4003,4002,91,10013,10015,931],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="3楼内二科大屏", type="leveldepart",
                       departmentid=[3001,4099,4001,4005,4013,3002,4014,458,459,8011,20007,927,69,4355,20002,920,20016,933],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="4楼生殖医学科妇科", type="leveldepart",
                       departmentid=[66,135,52,50,48,6,68,101,106,108,122,123,946],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="四楼妇产科大屏", type="leveldepart",
                       departmentid=[5201,5202,5204,2099,5016,5203,921,942,943,944,945,947],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="5楼口腔科大屏", type="leveldepart",
                       departmentid=[381,382,383,384,385],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="六楼皮肤科大屏", type="leveldepart",
                       departmentid=[402,404,406,408,302,461,940],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="3楼内分泌代谢中心诊室", type="leveldepart",
                       departmentid=[401,103,107,2112,104,929,926],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="5楼儿科大屏", type="leveldepart",
                       departmentid=[96,99,100],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="4楼生殖医学科男科大屏", type="leveldepart",
                       departmentid=[131,130],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            # Secondary triage examples
            Datasource(name="内科诊室二级分诊", type="secondarytriage", screenid=1,
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="外科诊室二级分诊", type="secondarytriage", screenid=2,
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            # Pharmacy
            Datasource(name="西药房一级", type="primarypharmacytriage",
                       pharmacydeptno=100, pharmacywinno=[1,2,3],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
            Datasource(name="西药房二级", type="secondarypharmacytriage",
                       pharmacydeptno=100, pharmacywinno=[1,2],
                       morningcleartime="00:00:00", afternooncleartime="00:00:00", ct=NOW, ut=NOW),
        ]
        db.add_all(datasources)
        await db.flush()  # Get IDs
        ds_map = {ds.name: ds.id for ds in datasources}
        print("✓ Datasources created")

        # ── Boxes (from log: realistic IPs, models, statuses) ──
        boxes = [
            # 3楼内一科
            Box(no="BOX044", name="3楼内一科大屏1", org=6, ip="192.168.105.216", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["3楼内一科大屏"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX045", name="3楼内一科大屏2", org=6, ip="192.168.105.217", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["3楼内一科大屏"]),
                status="正常", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # 3楼内二科
            Box(no="BOX105", name="3楼内二科大屏1", org=7, ip="192.168.102.208", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["3楼内二科大屏"]),
                status="断开", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX128", name="3楼内二科大屏2", org=7, ip="192.168.102.241", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["3楼内二科大屏"]),
                status="断开", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX130", name="3楼内二科大屏3", org=7, ip="192.168.102.242", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["3楼内二科大屏"]),
                status="断开", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # 5楼外科
            Box(no="BOX131", name="5楼外科大屏1", org=8, ip="192.168.104.153", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["5楼外科大屏"]),
                status="断开", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX160", name="5楼外科大屏2", org=8, ip="192.168.104.73", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["5楼外科大屏"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # 5楼耳鼻喉科
            Box(no="BOX055", name="5楼耳鼻喉科大屏", org=9, ip="192.168.104.119", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["5楼耳鼻喉科大屏"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # 6楼眼科
            Box(no="BOX046", name="6楼眼科大屏", org=4, ip="192.168.105.218", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["6楼眼科大屏"]),
                status="正常", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # 4楼
            Box(no="BOX161", name="4楼生殖医学科妇科大屏", org=2, ip="192.168.103.150", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["4楼生殖医学科妇科"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=8, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX164", name="四楼妇产科大屏1", org=2, ip="192.168.103.98", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["四楼妇产科大屏"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX166", name="四楼妇产科大屏2", org=2, ip="192.168.103.36", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["四楼妇产科大屏"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX168", name="5楼儿科大屏", org=3, ip="192.168.109.121", model="qzfe3128",
                appversion="2.4.7", style="leveldepart", datasource=str(ds_map["5楼儿科大屏"]),
                status="关机", powerontime="07:30:00", powerofftime="18:30:00", volume=7, rotation="0",
                horselamp="", dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # Secondary triage boxes
            Box(no="BOX200", name="内科诊室分诊屏1", org=6, ip="192.168.102.100", model="qzfe3128",
                appversion="2.4.7", style="secondarytriage", datasource=str(ds_map["内科诊室二级分诊"]),
                status="正常", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX201", name="内科诊室分诊屏2", org=6, ip="192.168.102.101", model="qzfe3128",
                appversion="2.4.7", style="secondarytriage", datasource=str(ds_map["内科诊室二级分诊"]),
                status="正常", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # Pharmacy boxes
            Box(no="BOX300", name="西药房叫号屏1", org=5, ip="192.168.102.200", model="qzfe3128",
                appversion="2.4.7", style="primarypharmacytriage", datasource=str(ds_map["西药房一级"]),
                status="正常", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            Box(no="BOX301", name="西药房取药屏", org=5, ip="192.168.102.201", model="qzfe3128",
                appversion="2.4.7", style="secondarypharmacytriage", datasource=str(ds_map["西药房二级"]),
                status="正常", powerontime="07:30:00", powerofftime="18:30:00", volume=9, rotation="0",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
            # 4楼生殖男科 (latest box from log)
            Box(no="BOX261", name="4楼生殖男科大屏", org=2, ip="192.168.103.117", model="qzfe3128",
                mac="02:00:00:00:00:00", appversion="2.4.7", style="leveldepart",
                datasource=str(ds_map["4楼生殖医学科男科大屏"]),
                status="正常", powerontime="00:00:00", powerofftime="00:00:00",
                dataenabled=1, ht=NOW, ct=NOW, ut=NOW),
        ]
        db.add_all(boxes)
        print("✓ Boxes created")

        # ── Templates ──
        templates = [
            Template(name="一级分诊-默认", key="primarytriage_default", kind="web", version="1.0.0",
                     html="""<div id="app">
  <div class="header"><span class="title">{{deptname}}</span><span class="doctor">{{doctorname}}</span></div>
  <div class="queue-list">
    {{#each queues}}
    <div class="queue-item">
      <span class="callno">{{callno}}</span>
      <span class="patient">{{patientname}}</span>
    </div>
    {{/each}}
  </div>
</div>""",
                     css="""*{margin:0;padding:0;box-sizing:border-box}
body{background:#0a0a1a;color:#fff;font-family:sans-serif}
.header{background:#1a1a2e;padding:20px;text-align:center}
.title{font-size:32px;color:#ffd700;margin-right:20px}
.doctor{font-size:24px;color:#aaa}
.queue-list{padding:20px}
.queue-item{display:flex;justify-content:space-between;padding:16px 20px;border-bottom:1px solid #222;font-size:24px}
.callno{color:#ffd700;font-size:28px;font-weight:bold}""",
                     ct=NOW, ut=NOW),
            Template(name="二级分诊-默认", key="secondarytriage_default", kind="web", version="1.0.0",
                     html="""<div id="app">
  <div class="header">
    <div class="clinic">{{clinicname}}</div>
    <div class="doctor-info">
      <span class="name">{{doctorname}}</span>
      <span class="title">{{doctortitle}}</span>
    </div>
  </div>
  <div class="patient-list">
    {{#each patients}}
    <div class="patient-item {{#if called}}called{{/if}}">
      <span class="ticket">{{ticket}}</span>
      <span class="name">{{brxmfull}}</span>
      <span class="status">{{statusText}}</span>
    </div>
    {{/each}}
  </div>
  <div class="count">等候人数: {{patientcount}}</div>
</div>""",
                     css="""*{margin:0;padding:0;box-sizing:border-box}
body{background:#0a0a1a;color:#fff;font-family:sans-serif}
.header{background:#1a1a2e;padding:30px;text-align:center}
.clinic{font-size:36px;color:#ffd700;margin-bottom:10px}
.doctor-info .name{font-size:28px;margin-right:15px}
.doctor-info .title{font-size:20px;color:#aaa}
.patient-list{padding:20px}
.patient-item{display:flex;justify-content:space-between;padding:14px 20px;border-bottom:1px solid #222;font-size:22px}
.patient-item.called{background:#1a3a1a;border-left:4px solid #4caf50}
.ticket{color:#ffd700;font-weight:bold;width:80px}
.count{padding:20px;text-align:center;color:#aaa;font-size:18px}""",
                     ct=NOW, ut=NOW),
            Template(name="药房叫号-默认", key="pharmacy_default", kind="web", version="1.0.0",
                     html="""<div id="app">
  <div class="header">
    <span class="window">窗口 {{winno}}</span>
    <span class="dept">{{deptname}}</span>
  </div>
  <div class="called-patient" id="called">
    <div class="ticket">{{calledTicket}}</div>
    <div class="name">{{calledName}}</div>
  </div>
  <div class="wait-list">
    {{#each waitPatients}}
    <div class="wait-item"><span class="ticket">{{no}}</span><span class="name">{{name}}</span></div>
    {{/each}}
  </div>
</div>""",
                     css="""*{margin:0;padding:0;box-sizing:border-box}
body{background:#0a0a1a;color:#fff;font-family:sans-serif}
.header{background:#1a1a2e;padding:20px;text-align:center}
.window{font-size:36px;color:#ffd700;margin-right:20px}
.dept{font-size:24px;color:#aaa}
.called-patient{background:#1a3a1a;padding:40px;text-align:center;margin:20px;border-radius:12px}
.ticket{font-size:72px;color:#ffd700;font-weight:bold}
.name{font-size:32px;margin-top:10px}
.wait-list{padding:20px}
.wait-item{display:flex;justify-content:space-between;padding:12px 20px;border-bottom:1px solid #222;font-size:20px}""",
                     ct=NOW, ut=NOW),
        ]
        db.add_all(templates)
        await db.flush()
        tmpl_map = {t.key: t.id for t in templates}
        print("✓ Templates created")

        # Assign templates to boxes
        for box in boxes:
            if "pharmacy" in (box.style or ""):
                box.template = str(tmpl_map.get("pharmacy_default", ""))
            elif "secondary" in (box.style or ""):
                box.template = str(tmpl_map.get("secondarytriage_default", ""))
            else:
                box.template = str(tmpl_map.get("primarytriage_default", ""))
        print("✓ Templates assigned to boxes")

        # ── System Config (placeholder) ──
        sys = SystemConfig(
            url="192.168.1.100:1521/orcl",
            primarytablename="VW_YS_JHLSXX_FQSYYXY",
            secondarytablename="VW_PDJH_HZBR00",
            secondarycounttablename="VW_PDJH_YSJZRS",
            drawbloodtablename="VW_JYCYJH",
            pharmacytablename="VW_PY_XSPPDJH_X",
            username="triage_user",
            password="***",
            pacssecondarytablename="V_QUEUE",
            pacsusername="pacs_user",
            pacspassword="***",
        )
        db.add(sys)
        print("✓ System config created")

        # ── Styles ──
        styles = [
            Style(name="一级分诊", key="primarytriage"),
            Style(name="二级分诊", key="secondarytriage"),
            Style(name="二级分诊(分屏)", key="secondarytriagesplit"),
            Style(name="二级超声分诊", key="secondarytriageultrasonic"),
            Style(name="检验分诊", key="drawbloodtriage"),
            Style(name="药房一级分诊", key="primarypharmacytriage"),
            Style(name="药房二级分诊", key="secondarypharmacytriage"),
            Style(name="层级科室", key="leveldepart"),
        ]
        db.add_all(styles)
        print("✓ Styles created")

        # ── Datasource Types ──
        dstypes = [
            DatasourceType(name="一级分诊", key="primarytriage"),
            DatasourceType(name="二级分诊", key="secondarytriage"),
            DatasourceType(name="二级分诊(分屏)", key="secondarytriagesplit"),
            DatasourceType(name="二级超声分诊", key="secondarytriageultrasonic"),
            DatasourceType(name="检验分诊", key="drawbloodtriage"),
            DatasourceType(name="药房一级分诊", key="primarypharmacytriage"),
            DatasourceType(name="药房二级分诊", key="secondarypharmacytriage"),
            DatasourceType(name="层级科室", key="leveldepart"),
        ]
        db.add_all(dstypes)
        print("✓ Datasource types created")

        await db.commit()
        print("\n✅ Seed complete!")
        print(f"   - 2 users (admin/admin123, nurse01/123456)")
        print(f"   - {len(orgs)} organizations")
        print(f"   - {len(datasources)} datasources")
        print(f"   - {len(boxes)} boxes")
        print(f"   - {len(templates)} templates")
        print(f"   - 1 system config")


if __name__ == "__main__":
    asyncio.run(seed())
